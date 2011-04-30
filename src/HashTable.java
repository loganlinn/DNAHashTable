import java.io.IOException;

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
	public static final int EMPTY_SLOT = -1;
	public static final int TOMBSTONE_SLOT = -2;

	public static final int BUCKET_SIZE = 32; // slots
	public static final int SLOT_SIZE = 4; // 4-byte integers

	public static final int ID_POS_OFFSET = 0;
	public static final int ID_LEN_OFFSET = 1;
	public static final int SEQ_POS_OFFSET = 2;
	public static final int SEQ_LEN_OFFSET = 3;
	public final int numSlots;
	public final MemoryManager memoryManager;
	public final int[] table;

	/**
	 * Constructs a HashTable
	 * 
	 * @param fileName
	 * @param numSlots
	 * @throws IOException
	 */
	public HashTable(String fileName, int numSlots) throws IOException {
		this.numSlots = numSlots;
		this.memoryManager = new MemoryManager(fileName);
		// Create hash table
		this.table = new int[numSlots * SLOT_SIZE];
		// Initialize the table to empty slot values
		for (int i = 0; i < numSlots; i++) {
			clearSlot(i);
		}

	}

	/**
	 * Inserts a sequence into the HashTable
	 * 
	 * @param sequenceID
	 * @param sequenceHandle
	 * @throws HashTableFullException
	 * @throws DuplicateSequenceException
	 */
	public void insert(String sequenceID, MemoryHandle sequenceHandle)
			throws HashTableFullException, DuplicateSequenceException {
		int homeSlot = sfold(sequenceID);
		int currentSlot = homeSlot;

		// boolean inserted = false;
		System.out.println("Inserting " + sequenceID);
		do {
			if (isSlotAvailable(currentSlot)) {
				System.out.println("  Took slot " + currentSlot);

				MemoryHandle handle = memoryManager.storeSequence(sequenceID);
				setSlot(currentSlot, handle.getSequenceFileOffset(),
						handle.getSequenceLength(), 0, 0);
				return;

			} else if (getSequenceIdLength(currentSlot) == sequenceID.length()) {
				// Check if this sequence is already stored
				if (sequenceID.equals(retrieveSequenceID(currentSlot))) {
					throw new DuplicateSequenceException(sequenceID);
				}
			}

			System.out
					.println("  Slot " + currentSlot + " taken, getting next");
			currentSlot = nextSlot(currentSlot);
		} while (currentSlot != homeSlot);

		// if (!inserted) {
		// throw new HashTableFullException();
		// }
		throw new HashTableFullException();
	}

	/**
	 * Removes a sequence from the HashTable
	 * 
	 * @param sequenceID
	 * @throws SequenceNotFoundException
	 */
	public void remove(String sequenceID) throws SequenceNotFoundException {
		int homeSlot = sfold(sequenceID);
		int currentSlot = homeSlot;
		do {
			if (isSlotEmpty(currentSlot)) {
				throw new SequenceNotFoundException(sequenceID);
			} else if (!isSlotTombstone(currentSlot)
					&& getSequenceIdLength(currentSlot) == sequenceID.length()) {
				// Check if we found it
				if (sequenceID.equals(retrieveSequenceID(currentSlot))) {
					printSequence(currentSlot);
					removeSlot(currentSlot);
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
	 */
	public void search(String sequenceID) throws SequenceNotFoundException {
		int homeSlot = sfold(sequenceID);
		int currentSlot = homeSlot;

		do {

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
		// TODO: IMPLEMENT!
	}

	/**
	 * Prints the contents of the HashTable
	 */
	public void print() {
		for (int i = 0; i < numSlots * SLOT_SIZE; i++) {
			System.out.println(i + " " + table[i]);
		}
		for (int slot = 0; slot < numSlots; slot++) {
			String slotVal;
			if (isSlotEmpty(slot)) {
				slotVal = "empty";
			} else if (isSlotTombstone(slot)) {
				slotVal = "tombstone";
			} else {
				slotVal = retrieveSequenceID(slot);
			}

			System.out.println(slot + " -> " + slotVal);
		}
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
	 */
	public void setSlot(int slot, int sequenceIdOffset, int sequenceIdLength,
			int sequenceOffset, int sequenceLength) {
		table[slot * SLOT_SIZE + ID_POS_OFFSET] = sequenceIdOffset;
		table[slot * SLOT_SIZE + ID_LEN_OFFSET] = sequenceIdLength;
		table[slot * SLOT_SIZE + SEQ_POS_OFFSET] = sequenceOffset;
		table[slot * SLOT_SIZE + SEQ_LEN_OFFSET] = sequenceLength;
	}

	/**
	 * Empties a slot
	 * 
	 * @param slot
	 */
	public void clearSlot(int slot) {
		setSlot(slot, EMPTY_SLOT, EMPTY_SLOT, EMPTY_SLOT, EMPTY_SLOT);
	}

	/**
	 * Removes a slot and replaces it with a tombstone
	 * 
	 * @param slot
	 */
	public void removeSlot(int slot) {
		setSlot(slot, TOMBSTONE_SLOT, TOMBSTONE_SLOT, TOMBSTONE_SLOT,
				TOMBSTONE_SLOT);
	}

	/**
	 * Returns length portion of sequence ID handle for stored in a given slot
	 * 
	 * @param slot
	 * @return
	 */
	protected int getSequenceIdLength(int slot) {
		return table[slot * SLOT_SIZE + ID_LEN_OFFSET];
	}

	/**
	 * Returns offset portion of sequence ID handle for a slot
	 * 
	 * @param slot
	 * @return
	 */
	protected int getSequenceIdOffset(int slot) {
		return table[slot * SLOT_SIZE + ID_POS_OFFSET];
	}

	/**
	 * Checks if a slot is empty
	 * 
	 * @param slot
	 * @return
	 */
	protected boolean isSlotEmpty(int slot) {
		return (table[slot * SLOT_SIZE] == EMPTY_SLOT);
	}

	/**
	 * Checks is a slot has been deleted and is a tombstone
	 * 
	 * @param slot
	 * @return
	 */
	protected boolean isSlotTombstone(int slot) {
		return (table[slot * SLOT_SIZE] == TOMBSTONE_SLOT);
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
