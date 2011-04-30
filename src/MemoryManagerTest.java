import java.io.IOException;

public class MemoryManagerTest {
	static MemoryManager sfm;

	public static void main(String[] args) {
//		llTest();
		mmTest();
	}
	
	public static void llTest(){
		LinkedList<Character> l = new LinkedList<Character>();
		
		l.insert('a');
		System.out.println(l);
		
		l.insert('b');
		System.out.println(l);
		
		l.remove();
		System.out.println(l);
		
		l.insert('b');
		l.insert('c');
		System.out.println(l);
		System.out.println("Size: "+l.length());
		
		System.out.println(l.remove('a'));
		System.out.println(l);
	}
	
	public static void mmTest() {
		String[] s = new String[] { "ACGT", "TTTTTTTT", "TGCA", "CCCC" };
		try {
			sfm = new MemoryManager("test.mm");

			MemoryHandle fh0 = store(s[0]);
			// SequenceFileHandle fh1 = store(s[1]);
			MemoryHandle fh2 = store(s[2]);
			MemoryHandle fh3 = store(s[3]);

			remove(fh0);

			 sfm.printFreeBlocks();

			remove(fh3);

			 sfm.printFreeBlocks();

			remove(fh2);

			 sfm.printFreeBlocks();

			store(s[1]);

			sfm.printFreeBlocks();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	public static void remove(MemoryHandle fh) {
		System.out.println("Removing: " + fh.toString());
		sfm.removeSequence(fh);
	}

	public static MemoryHandle store(String seq) {
		System.out.println("Storing: " + seq);
		return sfm.storeSequence(seq);
	}

	public static void retrieve(MemoryHandle fh) {
		String seq = sfm.retrieveSequence(fh);
		System.out.print("Recieved: ");
		byte[] data = seq.getBytes();
		for (byte b : data) {
			System.out.print(Integer.toHexString(b) + " ");
		}
		System.out.println(" = " + sfm.decode(data, fh.getSequenceLength()));
	}
}
