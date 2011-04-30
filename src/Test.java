import java.io.IOException;

public class Test {
	static MemoryManager sfm;

	public static void main(String[] args) {
//		llTest();
//		mmTest();
		htTest();
	}
	
	public static void htTest(){
		try {
			sfm = new MemoryManager("test.mm");
			HashTable ht = new HashTable("test.ht", 32, sfm);
			
			ht.insert("AA", sfm.storeSequence("ACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGTACGT"));
			ht.remove("AA");
			ht.insert("CC", sfm.storeSequence("AAAAAGGGCCCTTTA"));
			ht.search("CC");
			ht.search("CCC");
			
//			ht.print();
//			ht.printFreeBlocks();
//			sfm.printFreeBlocks();
			
			
//			ht.insert("C", null);
//			ht.insert("G", null);
//			ht.insert("T", null);
//			ht.insert("AA", null);
//			ht.insert("CC", null);
//			ht.insert("GG", null);
//			ht.insert("TT", null);
//			ht.insert("AAA", null);
//			ht.insert("CCC", null);
//			ht.insert("GGG", null);
//			ht.insert("TTT", null);
//			ht.insert("AAAA", null);
//			ht.insert("CCCC", null);
//			ht.insert("GGGG", null);
//			ht.insert("TTTT", null);
			/*
				Inserting A to slot 16
				Inserting C to slot 17
				Inserting G to slot 19
				Inserting T to slot 27
				Inserting AA to slot 18
				Inserting CC to slot 35
				Inserting GG to slot 53
				Inserting TT to slot 59
				Inserting AAA to slot 18
				Inserting CCC to slot 35
				Inserting GGG to slot 53
				Inserting TTT to slot 59
				Inserting AAAA to slot 18
				Inserting CCCC to slot 35
				Inserting GGGG to slot 53
				Inserting TTTT to slot 59
			 */

			
		} catch (IOException e) {
			e.printStackTrace();
		} catch (HashTableFullException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (DuplicateSequenceException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		} catch (SequenceNotFoundException e) {
			System.out.println(e.getMessage());
			e.printStackTrace();
		}
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
