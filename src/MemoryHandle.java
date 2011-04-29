/**
 * Represents a location in the sequence file for a particular sequence
 * 
 * @author loganlinn
 * 
 */
public class MemoryHandle {
	private final int sequenceLength;
	private final long sequenceFileOffset;

	/**
	 * 
	 * @param sequenceLength
	 * @param sequenceFileOffset
	 */
	public MemoryHandle(long sequenceFileOffset, int sequenceLength) {
		super();
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
	public long getSequenceFileOffset() {
		return sequenceFileOffset;
	}

	public String toString() {
		return "Handle: " + sequenceFileOffset + "+"
				+ MemoryManager.getEncodedSequenceLength(sequenceLength);
	}
}
