import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.StringTokenizer;

/**
 * Represents and parses a command file passed to the program. - Parse the
 * commands in command file for logical correctness - Create and collect Command
 * objects for the Tree - Notify the program of any other parsing errors
 * 
 * @author loganlinn
 * 
 */
public class CommandFile {
	/* Literal Constants */
	private static final String INSERT_COMMAND = "insert";
	private static final String REMOVE_COMMAND = "remove";
	private static final String PRINT_COMMAND = "print";
	private static final String SEARCH_COMMAND = "search";
	private static final String UNKNOWN_COMMAND_ERROR_PREFIX = "Unknown command, ";
	private static final String LINE_NUMBER_MESSAGE_PREFIX = "(Line ";
	private static final String LINE_NUMBER_MESSAGE_SUFFIX = ")";

	private String commandFilePath; // Path to command file
	private int lineNumber = 0; // Tracks which line of the command file we are
								// parsing

	private HashTable hashTable;
	private MemoryManager sequenceMemoryManager;

	/**
	 * Constructs a CommandFile given the path to a command file
	 * 
	 * @param path
	 */
	public CommandFile(String path, HashTable hashTable,
			MemoryManager sequenceMemoryManager) {
		commandFilePath = path;
		this.hashTable = hashTable;
		this.sequenceMemoryManager = sequenceMemoryManager;
	}

	/**
	 * Checks if the tokenizer has more tokens. If it does, return the next
	 * token, otherwise return null
	 * 
	 * @param tokenizer
	 * @return
	 */
	private String getNextArgument(StringTokenizer tokenizer) {
		if (tokenizer.hasMoreTokens()) {
			return tokenizer.nextToken();
		}
		return null;
	}

	/**
	 * Returns the next token in the string token, and parses it as an int
	 * 
	 * @param tokenizer
	 * @return
	 * @throws NumberFormatException
	 */
	private int getNextIntArgument(StringTokenizer tokenizer)
			throws NumberFormatException {
		if (tokenizer.hasMoreTokens()) {
			return Integer.parseInt(tokenizer.nextToken());
		}
		return -1;
	}

	/**
	 * Parses the command file Throws an appropriate exception if an error is
	 * encountered Checks for the following errors: - Invalid character in
	 * sequence - Unknown command - Expected argument missing
	 * 
	 * @throws SequenceException
	 * @throws IOException
	 * @throws P3Exception
	 */
	public void parse(HashTable table, MemoryManager memoryManager)
			throws IOException {

		File commandFile = new File(this.commandFilePath);

		BufferedReader br = new BufferedReader(new InputStreamReader(
				new DataInputStream(new FileInputStream(commandFile))));
		String line, command, argument = null;
		int length;
		while ((line = br.readLine()) != null) {
			lineNumber++;
			/**
			 * Supported commented out lines!
			 */
			if (line.startsWith("#")) {
				continue;
			}

			// Use a tokenizer to ignore whitespace and iterate trough command
			StringTokenizer lineTokens = new StringTokenizer(line);

			if (lineTokens.hasMoreTokens()) {
				command = lineTokens.nextToken();

				if (INSERT_COMMAND.equals(command)) {
					
					/*
					 * Insert command
					 */
					argument = getNextArgument(lineTokens);// sequenceId
					try {
						String sequence = br.readLine();
						lineNumber++;
						if(sequence == null || sequence.isEmpty()){
							System.out.println("Expecting a sequence!"+getLineNumberMessage());
							continue;
						}
						hashTable.insert(argument, sequenceMemoryManager
								.storeSequence(sequence));
					} catch (HashTableFullException e) {
						System.out.println(e.getMessage());
					} catch (DuplicateSequenceException e) {
						System.out.println(e.getMessage());
					}
					
				} else if (REMOVE_COMMAND.equals(command)) {
					
					/*
					 * Remove command
					 */
					argument = getNextArgument(lineTokens);
					try {
						hashTable.remove(argument);
					} catch (SequenceNotFoundException e) {
						System.out.println(e.getMessage());
					}
					
				} else if (PRINT_COMMAND.equals(command)) {
					
					/*
					 * Print command
					 */
					hashTable.print();
					
				} else if (SEARCH_COMMAND.equals(command)) {
					
					/*
					 * Search command, find the mode
					 */
					argument = getNextArgument(lineTokens); // argument is a
															// sequence
															// descriptor
					try {
						hashTable.search(argument);
					} catch (SequenceNotFoundException e) {
						System.out.println(e.getMessage());
					}
					
				} else {
					
					// The command isn't recognized, throw an exception
					throw new IOException(UNKNOWN_COMMAND_ERROR_PREFIX
							+ command + getLineNumberMessage());
				}

			}
		}

	}

	/**
	 * Format a message indicating which line number is currently being parsed
	 * 
	 * @return
	 */
	private String getLineNumberMessage() {
		return LINE_NUMBER_MESSAGE_PREFIX + lineNumber
				+ LINE_NUMBER_MESSAGE_SUFFIX;
	}

	/**
	 * @return the commandFilePath
	 */
	public String getCommandFilePath() {
		return commandFilePath;
	}

	/**
	 * @param commandFilePath
	 *            the commandFilePath to set
	 */
	public void setCommandFilePath(String commandFilePath) {
		this.commandFilePath = commandFilePath;
	}

}
