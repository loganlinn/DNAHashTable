
public class DuplicateSequenceException extends Exception {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6705957272243398553L;

	public DuplicateSequenceException(String sequenceID) {
		super(sequenceID+" already exists!");
	}

}
