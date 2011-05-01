import java.io.IOException;
import java.io.RandomAccessFile;

/**
 * Each slot of the hash table stores two memory handles: One memory handle is
 * for the sequenceID, and the other memory handle is for the sequence
 * 
 * Memory handles are two 4-byte integers: First is the position in the file of
 * the associated string, the 2nd is the length (in characters) of the string
 * 
 * Each bucket holds 512 bytes; This is 32 table slots since each slot stores
 * two memory handles. (a memory handle is 8-bytes in total)
 * 
 * @author loganlinn
 * 
 */
public class HashTable {
	protected static final int EMPTY_SLOT = -1;
	protected static final int TOMBSTONE_SLOT = -2;

	protected static final int BUCKET_SIZE = 32; // slots
	protected static final int SLOT_SIZE = 4; // 4-byte integers
	protected static final int SLOT_ITEM_BYTES = 4; // 4 bytes
	protected static final int SLOTS_IN_BUCKET = BUCKET_SIZE * SLOT_SIZE;
	protected static final int BYTES_IN_BUCKET = SLOTS_IN_BUCKET
			* SLOT_ITEM_BYTES;

	protected static final int ID_POS_OFFSET = 0;
	protected static final int ID_LEN_OFFSET = 1;
	protected static final int SEQ_POS_OFFSET = 2;
	protected static final int SEQ_LEN_OFFSET = 3;

	protected final int numSlots;
	protected final MemoryManager memoryManager;

	private int[] currentBucket;
	private int currentBucketNum = 0;
	private boolean currentBucketDirty = true;

	protected RandomAccessFile hashTable;

	/**
	 * Constructs a HashTable
	 * 
	 * @param fileName
	 * @param numSlots
	 * @throws IOException
	 */
	public HashTable(String fileName, int numSlots,
			MemoryManager sequenceFileMemoryManager) throws IOException {
		this.numSlots = numSlots;
		this.memoryManager = sequenceFileMemoryManager;
		// Create hash table
		hashTable = new RandomAccessFile(fileName, "rw");
		emptyHashTable();
	}

	private void emptyHashTable() throws IOException {
		hashTable.setLength(0);
		hashTable.setLength(numSlots * BYTES_IN_BUCKET);
		hashTable.seek(0);
		for (int i = 0; i < numSlots * SLOT_SIZE; i++) {
			hashTable.writeInt(EMPTY_SLOT);
		}
	}

	/**
	 * 
	 * @param slot
	 * @return
	 * @throws IOException
	 */
	public int[] readBucket(int slot) throws IOException {
		int bucketNum = slot / BUCKET_SIZE;

		if (currentBucketNum != bucketNum || currentBucketDirty) {
			int[] bucket = new int[SLOTS_IN_BUCKET];
			byte[] bucket_bytes = new byte[BYTES_IN_BUCKET];
//			System.out.println("Reading Bucket #" + bucketNum
//					+ (currentBucketDirty ? " (dirty)" : "") + " (slot:" + slot
//					+ ")");
			// Seek to beginning of bucket

			hashTable.seek((long) bucketNum * BYTES_IN_BUCKET);
			// Read entire bucket into memory
			hashTable.read(bucket_bytes);

			// Convert the bucket to integer array
			for (int i = 0; i < SLOTS_IN_BUCKET; i++) {
				bucket[i] = (bucket_bytes[i * SLOT_ITEM_BYTES] << 24)
						| (bucket_bytes[i * SLOT_ITEM_BYTES + 1] << 16)
						| (bucket_bytes[i * SLOT_ITEM_BYTES + 2] << 8)
						| (bucket_bytes[i * SLOT_ITEM_BYTES + 3]);
			}
			currentBucket = bucket;
			currentBucketNum = bucketNum;
			currentBucketDirty = false;

//			 for (int i = 0; i < bucket.length; i += 4) {
//			 System.out.println("[" + (i / 4) + "] " + bucket[i] + " "
//						+ bucket[i + 1] + " " + bucket[i + 2] + " "
//						+ bucket[i + 3]);
//			 }
		}

		return currentBucket;
	}

	/**
	 * Inserts a sequence into the HashTable
	 * 
	 * @param sequenceID
	 * @param sequenceHandle
	 * @throws HashTableFullException
	 * @throws DuplicateSequenceException
	 * @throws IOException
	 */
	public void insert(String sequenceID, MemoryHandle sequenceIdHandle,
			MemoryHandle sequenceHandle) throws HashTableFullException,
			DuplicateSequenceException, IOException {

		int homeSlot = sfold(sequenceID);
		int currentSlot = homeSlot;

		// boolean inserted = false;
//		 System.out.println("Inserting " +
//		 sequenceID+" "+sequenceHandle.getSequenceLength());
		readBucket(homeSlot);
		do {
			if (isSlotAvailable(currentSlot)) {
				// System.out.println("  Took slot " + currentSlot);

				writeSlot(currentSlot, sequenceIdHandle.getByteOffset(),
						sequenceIdHandle.getSequenceLength(),
						sequenceHandle.getByteOffset(),
						sequenceHandle.getSequenceLength());

				if ((int) currentSlot / BUCKET_SIZE == currentBucketNum) {
					currentBucketDirty = true;
				}

				return;

			} else if (getSequenceIdLength(currentSlot) == sequenceID.length()) {
				// Check if this sequence is already stored
				if (sequenceID.equals(retrieveSequenceID(currentSlot))) {
					throw new DuplicateSequenceException(sequenceID);
				}
			}

			// System.out
			// .println("  Slot " + currentSlot + " taken, getting next");
			currentSlot = nextSlot(currentSlot);
		} while (currentSlot != homeSlot);

		throw new HashTableFullException();
	}

	/**
	 * Removes a sequence from the HashTable
	 * 
	 * @param sequenceID
	 * @throws SequenceNotFoundException
	 * @throws IOException
	 */
	public void remove(String sequenceID) throws SequenceNotFoundException,
			IOException {
		int homeSlot = sfold(sequenceID);
		int currentSlot = homeSlot;
		readBucket(homeSlot);
		do {
			if (isSlotEmpty(currentSlot)) {
				throw new SequenceNotFoundException(sequenceID);
			} else if (!isSlotTombstone(currentSlot)
					&& getSequenceIdLength(currentSlot) == sequenceID.length()) {
				// Check if we found it
				if (sequenceID.equals(retrieveSequenceID(currentSlot))) {
					// Print the full sequence
					System.out.println("Sequence Removed: "+sequenceID);
					printSequence(currentSlot);
					
					// Remove the sequence ID from MM
					memoryManager.removeSequence(
							getSequenceIdOffset(currentSlot),
							getSequenceIdLength(currentSlot));
					// Remove the sequence from MM
					memoryManager.removeSequence(
							getSequenceOffset(currentSlot),
							getSequenceLength(currentSlot));
					// Clear the slot in the hash table
					removeSlot(currentSlot);

					if ((int) currentSlot / BUCKET_SIZE == currentBucketNum) {
						currentBucketDirty = true;
					}

					return;
				}
			}
			currentSlot = nextSlot(currentSlot);
		} while (currentSlot != homeSlot);

		// Bucket is full, and sequenceID wasn't found
		throw new SequenceNotFoundException(sequenceID);
	}

	/**
	 * Searches for a sequence in the HashTable
	 * 
	 * @param sequenceID
	 * @throws SequenceNotFoundException
	 * @throws IOException
	 */
	public void search(String sequenceID) throws SequenceNotFoundException,
			IOException {
		int homeSlot = sfold(sequenceID);
		int currentSlot = homeSlot;
		readBucket(homeSlot);
		do {
			if (isSlotEmpty(currentSlot)) {
				throw new SequenceNotFoundException(sequenceID);
			} else if (!isSlotTombstone(currentSlot)
					&& getSequenceIdLength(currentSlot) == sequenceID.length()) {
				// Check if we found it
				if (sequenceID.equals(retrieveSequenceID(currentSlot))) {
					// Print the full sequence
					printSequence(currentSlot);
					return;
				}
			}
			currentSlot = nextSlot(currentSlot);
		} while (currentSlot != homeSlot);

		throw new SequenceNotFoundException(sequenceID);
	}

	/**
	 * Prints a full sequence located in the indicated slot
	 * 
	 * @param slot
	 */
	public void printSequence(int slot) {
		String sequence = memoryManager.retrieveSequence(
				getSequenceOffset(slot), getSequenceLength(slot));
		System.out.println("Printing "+getSequenceOffset(slot)+" "+getSequenceLength(slot));
		System.out.println(sequence);
	}

	/**
	 * Prints the contents of the HashTable
	 * 
	 * @throws IOException
	 */
	public void print() throws IOException {
		System.out.println("SequenceIDs:");
		for (int slot = 0; slot < numSlots; slot++) {
			// Retrieve each bucket from hash file
			if (slot % BUCKET_SIZE == 0) {
				readBucket(slot);
			}
			if (!isSlotAvailable(slot)) {
				System.out.print("  ");
				System.out.print(retrieveSequenceID(slot));
				System.out.println(": hash slot [" + slot + "]");
			}
		}
		memoryManager.printFreeBlocks();
	}

	/**
	 * Returns the sequence ID stored for a given slot
	 * 
	 * @param slot
	 * @return
	 */
	public String retrieveSequenceID(int slot) {
		// Check if a sequence id is actually stored in this slot
		if (isSlotAvailable(slot)) {
			return null;
		}
		return memoryManager.retrieveSequence(getSequenceIdOffset(slot),
				getSequenceIdLength(slot));
	}

	/**
	 * Assigns a slot with two memory handles
	 * 
	 * @param slot
	 * @param sequenceIdOffset
	 * @param sequenceIdLength
	 * @param sequenceOffset
	 * @param sequenceLength
	 * @throws IOException
	 */
	public void writeSlot(int slot, MemoryHandle sequenceIdHandle,
			MemoryHandle sequenceHandle) throws IOException {
		writeSlot(slot, sequenceIdHandle.getByteOffset(),
				sequenceIdHandle.getSequenceLength(),
				sequenceHandle.getByteOffset(),
				sequenceHandle.getSequenceLength());
	}

	public void writeSlot(int slot, int sequenceIdOffset, int sequenceIdLength,
			int sequenceOffset, int sequenceLength) throws IOException {
		// System.out.println("Writing "+sequenceIdOffset+" "+sequenceIdLength+" "+sequenceOffset+" "+sequenceLength);
		hashTable.seek(slot * SLOT_SIZE * SLOT_ITEM_BYTES);
		hashTable.writeInt(sequenceIdOffset);
		hashTable.writeInt(sequenceIdLength);
		hashTable.writeInt(sequenceOffset);
		hashTable.writeInt(sequenceLength);
	}

	/**
	 * Empties a slot
	 * 
	 * @param slot
	 * @throws IOException
	 */
	public void clearSlot(int slot) throws IOException {
		writeSlot(slot, EMPTY_SLOT, EMPTY_SLOT, EMPTY_SLOT, EMPTY_SLOT);
	}

	/**
	 * Removes a slot and replaces it with a tombstone
	 * 
	 * @param slot
	 * @throws IOException
	 */
	public void removeSlot(int slot) throws IOException {
		writeSlot(slot, TOMBSTONE_SLOT, TOMBSTONE_SLOT, TOMBSTONE_SLOT,
				TOMBSTONE_SLOT);
	}

	/**
	 * Returns length portion of sequence ID handle for stored in a given slot
	 * 
	 * @param slot
	 * @return
	 */
	protected int getSequenceIdLength(int slot) {
		return currentBucket[currentBucketInd(slot) + ID_LEN_OFFSET];
	}

	/**
	 * Returns offset portion of sequence ID handle for a slot
	 * 
	 * @param slot
	 * @return
	 */
	protected int getSequenceIdOffset(int slot) {
		return currentBucket[currentBucketInd(slot) + ID_POS_OFFSET];
	}

	/**
	 * Returns length portion of sequence ID handle for stored in a given slot
	 * 
	 * @param slot
	 * @return
	 */
	protected int getSequenceLength(int slot) {
		return currentBucket[currentBucketInd(slot) + SEQ_LEN_OFFSET];
	}

	/**
	 * Returns offset portion of sequence ID handle for a slot
	 * 
	 * @param slot
	 * @return
	 */
	protected int getSequenceOffset(int slot) {
		return currentBucket[currentBucketInd(slot) + SEQ_POS_OFFSET];
	}

	/**
	 * Checks if a slot is empty
	 * 
	 * @param slot
	 * @return
	 */
	protected boolean isSlotEmpty(int slot) {
		return (currentBucket[currentBucketInd(slot)] == EMPTY_SLOT);
	}

	/**
	 * Checks is a slot has been deleted and is a tombstone
	 * 
	 * @param slot
	 * @return
	 */
	protected boolean isSlotTombstone(int slot) {
		return (currentBucket[currentBucketInd(slot)] == TOMBSTONE_SLOT);
	}

	/**
	 * Gets the index in the bucket data for an arbitrary slot
	 * 
	 * @param slot
	 * @return
	 */
	protected int currentBucketInd(int slot) {
		// System.out.println("  "+slot+"->"+((slot % BUCKET_SIZE) *
		// SLOT_SIZE));
		return (slot % BUCKET_SIZE) * SLOT_SIZE;
	}

	/**
	 * Checks if a slot can be taken
	 * 
	 * @param slot
	 * @return
	 */
	protected boolean isSlotAvailable(int slot) {
		return isSlotEmpty(slot) || isSlotTombstone(slot);
	}

	/**
	 * Gets next slot to probe during collision resolution
	 * 
	 * @param currentSlot
	 * @return
	 */
	public int nextSlot(int currentSlot) {
		if (currentSlot % BUCKET_SIZE == BUCKET_SIZE - 1) {
			return (currentSlot - BUCKET_SIZE + 1);
		} else {
			return (currentSlot + 1);
		}
	}

	/**
	 * Hashes sequenceIDs
	 * 
	 * @param s
	 * @param M
	 * @return
	 */
	private int sfold(String s) {
		int intLength = s.length() / 4;
		long sum = 0;
		for (int j = 0; j < intLength; j++) {
			char c[] = s.substring(j * 4, (j * 4) + 4).toCharArray();
			for (int k = 0; k < c.length; k++) {
				sum += c[k] << (8 * k);
			}
		}

		char c[] = s.substring(intLength * 4).toCharArray();
		for (int k = 0; k < c.length; k++) {
			sum += c[k] << (8 * k);
		}

		sum = (sum * sum) >> 8;
		return (int) (Math.abs(sum) % this.numSlots);
	}
}
