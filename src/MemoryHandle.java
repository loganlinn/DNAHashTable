/**
 * Represents a location in the sequence file for a particular sequence
 * 
 * @author loganlinn
 * 
 */
public class MemoryHandle {
	private final int sequenceLength;
	private final int sequenceFileOffset;

	/**
	 * 
	 * @param sequenceLength
	 * @param sequenceFileOffset
	 */
	public MemoryHandle(int sequenceFileOffset, int sequenceLength) {
		this.sequenceLength = sequenceLength;
		this.sequenceFileOffset = sequenceFileOffset;
	}

	/**
	 * @return the sequenceLength
	 */
	public int getSequenceLength() {
		return sequenceLength;
	}

	/**
	 * @return the sequenceFileOffset
	 */
	public int getByteOffset() {
		return sequenceFileOffset;
	}

	public String toString() {
		return "Handle: " + sequenceFileOffset + "+"
				+ MemoryManager.getEncodedSequenceLength(sequenceLength);
	}
}
