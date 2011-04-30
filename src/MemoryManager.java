import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.StringReader;

/**
 * Manages the file that stores long sequences using first fit approach.
 * 
 * Memory Manger contains a nested inner class, {@link FirstFitList} for
 * managing the free sections of the binary file.
 * 
 * @author loganlinn
 * 
 */
public class MemoryManager {
	private File sequenceFile;
	private RandomAccessFile seqAccess;
	private FirstFitList firstFit = new FirstFitList();

	/**
	 * Creates the manager
	 * 
	 * @throws IOException
	 */
	public MemoryManager(String fileName) throws IOException {
		sequenceFile = new File(fileName);
		seqAccess = new RandomAccessFile(sequenceFile, "rw");
		seqAccess.setLength(0); // clear out the file
	}

	/**
	 * Insert a sequence into the SequenceFile, returning a handle to the
	 * sequence
	 * 
	 * @param sequence
	 * @return
	 */
	public MemoryHandle storeSequence(String sequenceDescriptor) {
		int sequenceBlockLength = getEncodedSequenceLength(sequenceDescriptor
				.length());

		int byteOffset = firstFit.allocateBlock(sequenceBlockLength);

		// System.out.println("  storing @ " + byteOffset);

		try {

			// RandomAccessFile raf = new RandomAccessFile(sequenceFile, "rw");

			seqAccess.seek(byteOffset);

			seqAccess.write(encodeString(sequenceDescriptor,
					sequenceBlockLength));

			// seqAccess.close();

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return new MemoryHandle(byteOffset, sequenceDescriptor.length());
	}

	/**
	 * Releases the block described by the handle (marks it as free). And
	 * returns the underlying sequence
	 * 
	 * @param handle
	 * @return
	 */
	public String removeSequence(MemoryHandle handle) {
		firstFit.releaseBlock(handle);
		return retrieveSequence(handle);
	}
	public String removeSequence(int offset, int length){
		firstFit.releaseBlock(offset, length);
		return retrieveSequence(offset, length);
	}

	/**
	 * Reads the database for the sequence stored at the location described by
	 * the memory
	 * 
	 * @param handle
	 * @return
	 */
	public String retrieveSequence(MemoryHandle handle) {
		return retrieveSequence(handle.getSequenceFileOffset(), handle.getSequenceLength());
	}
	public String retrieveSequence(int offset, int length) {
//		System.out.println("retrieveSequence("+offset+", "+length+")");// Debug message
		int bytesToRead = getEncodedSequenceLength(length);
		byte[] sequenceBuffer = new byte[bytesToRead]; // Create a buffer to
														// store the sequence
		try {

			seqAccess.seek(offset);

			seqAccess.read(sequenceBuffer);

		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return decode(sequenceBuffer, length);
	}
	
	/**
	 * Gets the number of bytes: eg ceil(data/4) without need to cast to cast or
	 * divide
	 * 
	 * @param sequence
	 */
	public static int getEncodedSequenceLength(int sequenceLength) {
		return ((sequenceLength & 0x3) != 0 ? 1 : 0) + (sequenceLength >> 2);
	}

	/**
	 * Encodes a sequence 4 characters to byte
	 * 
	 * @param sequence
	 * @param numBytes
	 * @return
	 */
	private static byte[] encodeString(String sequence, int numBytes) {
		byte[] output = new byte[numBytes];
		StringReader reader = new StringReader(sequence);
		char[] buffer = new char[4];
		int charsRead, bytesEncoded = 0;
		byte encodedValue = 0x0;
		try {
			while ((charsRead = reader.read(buffer)) != -1) {

				/*
				 * Loop through the 4 chars
				 */
				for (int i = charsRead - 1; i >= 0; i--) {
					switch (buffer[i]) {
					case 'A':
						encodedValue = 0x0;
						break;
					case 'C':
						encodedValue = 0x1;
						break;
					case 'G':
						encodedValue = 0x2;
						break;
					case 'T':
						encodedValue = 0x3;
						break;
					}

					output[bytesEncoded] = (byte) ((output[bytesEncoded] << 2) | encodedValue);

				}

				bytesEncoded++;
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		// System.out.print("    Encoded "+sequence+" to ");
		// for(byte b : output){
		// System.out.print(Integer.toHexString(b)+" ");
		// }
		// System.out.println("");
		return output;
	}

	/**
	 * Decodes 1 byte to 4 sequence characters
	 * 
	 * @param data
	 * @param length
	 * @return
	 */
	public static String decode(byte[] data, int length) {
		StringBuilder sb = new StringBuilder();
		// if (getEncodedSequenceLength(length) > data.length) {
		// System.err.println("Length does not match data!");
		// return null;
		// }
		int byteInd = 0, charsDecoded = 0;
		while (charsDecoded < length) {
			byte b = data[byteInd];
			for (int i = 0; i < 4; i++) {
				switch (b & 0x3) {
				case 0x0:
					sb.append('A');
					break;
				case 0x1:
					sb.append('C');
					break;
				case 0x2:
					sb.append('G');
					break;
				case 0x3:
					sb.append('T');
					break;
				}
				if (++charsDecoded >= length)
					break;
				b >>= 2;
			}
			byteInd++;
		}
		return sb.toString();
	}

	/**
	 * Closes the random access file
	 * 
	 * @throws IOException
	 */
	public void close() throws IOException {
		seqAccess.close();
	}

	/**
	 * A wrapper method for printing our free block
	 */
	public void printFreeBlocks() {
		firstFit.print();
	}

	/**
	 * Class to implement First-Fit algorithm for determining where to place
	 * blocks.
	 * 
	 * When the memory manager requests(allocates) spaces for a sequence, the
	 * FirstFitList will find the first free block large enough to accommodate
	 * the sequence size specified by the MemoryManager.
	 * 
	 * See method descriptions below for implementation details.
	 * 
	 * Uses nested inner class {@link FreeBlock} to represent offset+size of a
	 * block
	 * 
	 * @author loganlinn
	 * 
	 */
	private class FirstFitList {
		private LinkedList<FreeBlock> freeBlocks;

		/**
		 * Constructs a FirstFitList
		 */
		public FirstFitList() {
			freeBlocks = new LinkedList<FreeBlock>();
		}

		/**
		 * Releases the memory allocated at the offset and size indicated in the
		 * MemoryHandle.
		 * 
		 * The newly released space is not written to, but simply marked as
		 * free. The released space's data is overwritten when the block is
		 * re-allocated
		 * 
		 * Combines adjacent free blocks to form larger free blocks.
		 * 
		 * Goal: keep the free list at minimal length with blocks of maximal
		 * size
		 * 
		 * Release Scenarios:
		 * 
		 * -- There is a free block immediately before the released handle. ->
		 * increase size of free block
		 * 
		 * -- There is a free block immediately after the released handle ->
		 * increase size of free block, free block offset becomes offset of the
		 * released file handle
		 * 
		 * -- There is a free block immediately before AND after the released
		 * handle -> remove the free block succeeds the released handle,
		 * increase the size of the first free block by the size of the released
		 * handle's size and the removed free block's size. (A merge)
		 * 
		 * -- There are no free blocks immediately before OR after -> ---- Free
		 * list is empty -> create new block and insert into list ---- There are
		 * sequences stored before and after the handle -> Create a new handle
		 * and insert it BEFORE the first block with a greater offset. This
		 * keeps the list of free blocks in order!
		 * 
		 * @param handle
		 */
		public void releaseBlock(MemoryHandle handle) {
			releaseBlock(handle.getSequenceFileOffset(), handle.getSequenceLength());
		}
		public void releaseBlock(int offset, int length) {
			int size = MemoryManager.getEncodedSequenceLength(length);
			int end = size + offset;

			/*
			 * Find blocks that could be merged
			 */
			FreeBlock prevBlock = null;
			FreeBlock nextBlock = null;
			freeBlocks.moveToStart();
			while (freeBlocks.hasNext()) {
				
				FreeBlock currBlock = freeBlocks.getValue();

				if (currBlock.getEnd() == offset) {
					prevBlock = currBlock;

					// Break if we have already found our next block
					if (nextBlock != null) {
						break;
					}
				} else if (currBlock.getOffset() == end) {
					nextBlock = currBlock;

					// Break if we have already found our previous block
					if (prevBlock != null) {
						break;
					}
				} else if (currBlock.getOffset() > end) { // No merges possible
					/*
					 * Move iterator back so we can INSERT right BEFORE
					 * currBlock which has a LARGER offset
					 */
					freeBlocks.prev();
					break;
				}
				freeBlocks.next();
			}

			/*
			 * Determine which release scenario we are in.
			 */
			if (prevBlock != null && nextBlock != null) { // Merge
				freeBlocks.remove(nextBlock);
				prevBlock.addToEnd(size + nextBlock.getSize());
			} else if (prevBlock != null) { // A block exists immediately before
				prevBlock.addToEnd(size);
			} else if (nextBlock != null) { // A block exists immediately after
				nextBlock.addToFront(size);
			} else { // No merging possible
				/*
				 * If lists is empty, iterator will add to beginning. Else, we
				 * are inserting before the bigger
				 */
				freeBlocks.insert(new FreeBlock(size, offset));
			}

		}

		/**
		 * Allocates space for an encoded sequence, given its size in bytes.
		 * 
		 * Allocation Sequence:
		 * 
		 * -> Step though the free list, looking for a block with the same or
		 * larger size as the requested size
		 * 
		 * -- If we find a block with the exact same size ad the requested size,
		 * remove the free block from the list
		 * 
		 * -- If we find a block larger than requested size, reduce its size by
		 * MOVING its offset FORWARD (ie take the first part of the sequence)
		 * 
		 * -- If a large enough block does not exist, the FirstFitList returns
		 * an offset beyond the end of the file. When this occurs, if there is a
		 * free block at the end of the list (previously determined to be too
		 * small to fully contain the allocated space), the FreeList returns the
		 * offset of the beginning of that free block.
		 * 
		 * 
		 * Return the byte offset in the file where the sequence will be stored
		 * 
		 * @param blockSize
		 *            in bytes
		 * @return
		 */
		public int allocateBlock(int blockSize) {
			FreeBlock block = null;
			freeBlocks.moveToStart();
			while (freeBlocks.hasNext()) {
				block = freeBlocks.getValue();
				if (block.getSize() == blockSize) {
					/*
					 * If sizes are exact matches, we consume this block
					 */
					freeBlocks.remove();
					return block.getOffset();
				} else if (block.getSize() > blockSize) {
					int offset = block.getOffset();
					block.takeFromFront(blockSize);
					return offset;
				}
				freeBlocks.next();
			}

			/*
			 * If we've gotten this far, we need to expand the file. Need to
			 * return offset of EOF plus any extra space that was too small.
			 * Check if the last free block is at end of file.
			 * 
			 * If block is null, there are no free blocks, so we store at end
			 */
			int eof = (int) sequenceFile.length();
			if (block != null && block.getEnd() >= eof) {
				eof -= block.getSize(); // take this free block
				freeBlocks.remove();
			}
			return eof;
		}

		/**
		 * Print the FirstFit's free blocks
		 */
		public void print() {
			if (freeBlocks.length() == 0) {
				System.out.println("  Free Block List: none");
			} else {
				System.out.println("  Free Block List:");
				System.out.println(freeBlocks);
			}
		}

		/**
		 * Represents a free area in the file
		 * 
		 * @author loganlinn
		 * 
		 */
		private class FreeBlock {
			private int size; // block size in bytes
			private int offset; // location in file

			/**
			 * Constructs a FreeBlock
			 * 
			 * @param size
			 *            of block in BYTES
			 * @param offset
			 *            from beginning of file in BYTES
			 */
			public FreeBlock(int size, int offset) {
				super();
				this.size = size;
				this.offset = offset;
			}

			/**
			 * @return the size
			 */
			public int getSize() {
				return size;
			}

			/**
			 * @return the offset
			 */
			public int getOffset() {
				return offset;
			}

			/**
			 * @return the offset of the end of the free block
			 */
			public int getEnd() {
				return offset + size;
			}

			/**
			 * Expand the free block's size from the end by step amount
			 * 
			 * @param step
			 *            size in bytes
			 */
			public void addToEnd(int step) {
				size += step;
			}

			/**
			 * Expand the free block's size from the start by step amount
			 * 
			 * @param step
			 *            size in bytes
			 */
			public void addToFront(int step) {
				size += step;
				offset -= step;
			}

			/**
			 * Reduce the free block's size from the start by step amount. Move
			 * the beginning offset forward by step amount.
			 * 
			 * @param step
			 *            size in bytes
			 */
			public void takeFromFront(int step) {
				size -= step;
				offset += step;
			}

			/**
			 * Print a message representing this free block
			 */
			public String toString() {
				return "[Offset: " + offset + ", Size:" + size + "]";
			}
		} /* end FreeBlock */
	} /* end FirstFitList */

} /* end MemoryManager */
