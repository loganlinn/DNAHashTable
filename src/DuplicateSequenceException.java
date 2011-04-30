
public class DuplicateSequenceException extends Exception {

	public DuplicateSequenceException(String sequenceID) {
		super(sequenceID+" already exists!");
	}

}
