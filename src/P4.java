import java.io.IOException;

/**
 * DNA HASH TABLES CS3114 Project 4
 * 
 * == RUNNING ==
 * 
 * P4 <command-file> <hash-file> <hash-table-size> <memory-file>
 * 
 * 
 * @author loganlinn
 * 
 */
public class P4 {

	/**
	 * On my honor:
	 * 
	 * - I have not used source code obtained from another student, or any other
	 * unauthorized source, either modified or unmodified.
	 * 
	 * - All source code and documentation used in my program is either my
	 * original, or was derived by me from the source code published in the
	 * textbook for this course.
	 * 
	 * - I have not discussed coding details about this project with anyone
	 * other than my partner (in the case of a join submission), instructor,
	 * ACM/UPE tutors or the TAs assigned to this course. I understand that I
	 * may discuss the concepts of this program with other students, and that
	 * other students may help me debug my program so long as neither of us
	 * writes anything during the discussion or modifies any computer file
	 * during the discussion. I have violated neither in the spirit nor the
	 * letter of this restriction.
	 * 
	 * == Logan Linn ==
	 * 
	 * COMPILER: Eclipse JDT
	 * 
	 * OS: Mac OS X 10.6.7
	 * 
	 * == Matthew Ibarra ==
	 * 
	 * COMPILER: Eclipse JDT
	 * 
	 * OS: Mac OS X 10.6.6
	 * 
	 * == DATE COMPLETED == TODO: fill in date completed
	 * 
	 * @param args
	 */
	public static final int IND_COMMAND_FILE = 0;
	public static final int IND_HASH_FILE = 1;
	public static final int IND_HASH_TABLE_SIZE = 2;
	public static final int IND_MEMORY_FILE = 3;

	public static void main(String[] args) throws IOException {
		MemoryManager sequenceFileMemoryManager = new MemoryManager(
				args[IND_MEMORY_FILE]);
		
		int hashTableSize = Integer.parseInt(args[IND_HASH_TABLE_SIZE]);
		
		HashTable hashTable = new HashTable(args[IND_HASH_FILE],
				hashTableSize, sequenceFileMemoryManager);
		
		CommandFile commandFile = new CommandFile(args[IND_COMMAND_FILE],
				hashTable, sequenceFileMemoryManager);
		
		commandFile.parse();

	}

}
