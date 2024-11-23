/*  Student information for assignment:
 *
 *  On <MY|OUR> honor, <NAME1> and <NAME2), this programming assignment is <MY|OUR> own work
 *  and <I|WE> have not provided this code to any other student.
 *
 *  Number of slip days used:
 *
 *  Student 1 (Student whose Canvas account is being used)
 *  UTEID:
 *  email address:
 *  Grader name:
 *
 *  Student 2
 *  UTEID:
 *  email address:
 *
 */

// TODO style: no bytes (only ints); method comments; test empty file; test one char file

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;

import javax.management.RuntimeErrorException;

public class SimpleHuffProcessor implements IHuffProcessor {
    private IHuffViewer myViewer;
    private PriorityQueue314<TreeNode> charFreqs;
    private int[] freqs;
    private HashMap<Integer, String> codeSequences;
    private boolean spaceSaved;
    private boolean isTreeFormat;

    public SimpleHuffProcessor() {
        charFreqs = new PriorityQueue314<>();
        codeSequences = new HashMap<>();
    }

    /**
     * Preprocess data so that compression is possible ---
     * count characters/create tree/store state so that
     * a subsequent call to compress will work. The InputStream
     * is <em>not</em> a BitInputStream, so wrap it in one as needed.
     * 
     * @param in           is the stream which could be subsequently compressed
     * @param headerFormat a constant from IHuffProcessor that determines what kind
     *                     of
     *                     header to use, standard count format, standard tree
     *                     format, or
     *                     possibly some format added in the future.
     * @return number of bits saved by compression or some other measure
     *         Note, to determine the number of
     *         bits saved, the number of bits written includes
     *         ALL bits that will be written including the
     *         magic number, the header format number, the header to
     *         reproduce the tree, AND the actual data.
     * @throws IOException if an error occurs while reading from the input file.
     */
    public int preprocessCompress(InputStream in, int headerFormat) throws IOException {
        if (headerFormat != STORE_COUNTS && headerFormat != STORE_TREE) {
            throw new IllegalArgumentException("Parameter headerFormat must equal "
                    + "IHuffConstants.STORE_COUNTS or IHuffConstants.STORE_TREE");
        }

        BitInputStream bitIn = new BitInputStream(in);
        freqs = new int[ALPH_SIZE];
        int bit = bitIn.readBits(BITS_PER_WORD);
        int bitsInOriginal = 0;

        // find frequencies of each character
        while (bit != -1) {
            bitsInOriginal++;
            freqs[bit]++;
            bit = bitIn.readBits(BITS_PER_WORD);
        }

        bitIn.close();
        bitsInOriginal *= BITS_PER_WORD;

        int bitsInCompressed = BITS_PER_INT * 2;
        isTreeFormat = headerFormat == STORE_TREE;
        bitsInCompressed += buildTree(freqs); // TODO change this into class?
        findCodes(charFreqs.peek(), codeSequences, "");
        bitsInCompressed += isTreeFormat ? BITS_PER_INT * freqs.length : 0;

        // the actual data
        for (int character : codeSequences.keySet()) { // TODO traverse through freqs instead
            if (character != ALPH_SIZE) {
                bitsInCompressed += codeSequences.get(character).length() * freqs[character];
            }
        }

        bitsInCompressed += codeSequences.get(PSEUDO_EOF).length();

        showString((bitsInOriginal - bitsInCompressed) + " bits saved.");
        System.out.println(bitsInOriginal + " bits in original, " + bitsInCompressed + " bits in compressed");
        System.out.println(
                (bitsInOriginal / 8) + " bytes in original, " + (bitsInCompressed / 8) + " bytes in compressed");
        System.out.println((bitsInOriginal - bitsInCompressed) + " bits saved detected in SimpleHuffProcessor");
        spaceSaved = bitsInOriginal > bitsInCompressed;
        // myViewer.update("Still not working");
        return bitsInOriginal - bitsInCompressed;
    }

    /**
     * 
     * @param freqs
     * @return the number of bits required to store this tree using standard tree
     *         format
     */
    private int buildTree(int[] freqs) {
        // accounts for the bits used to store the int that represents the size of
        // the data that stores the tree

        // add frequencies into PQ
        for (int ch = 0; ch < freqs.length; ch++) {
            charFreqs.enqueue(new TreeNode(ch, freqs[ch]));
        }
        charFreqs.enqueue(new TreeNode(PSEUDO_EOF, 1)); // add PEOF to PQ

        // System.out.println("\nqueue:");
        // charFreqs.print(); // delete

        // // testing (delete)
        // int numCharacters = 0;
        // for (int ch = 0; ch <= freqs.length; ch++) {
        // TreeNode temp = charFreqs.dequeue();
        // System.out.print(temp + " ");
        // numCharacters += temp.getFrequency();
        // }
        // System.out.println("\nchar count by counting: " + numCharacters);
        // // reset tree
        // for (int ch = 0; ch < freqs.length; ch++) {
        // charFreqs.enqueue(new TreeNode(ch, freqs[ch]));
        // }
        // charFreqs.enqueue(new TreeNode(PSEUDO_EOF, 1));

        int bitsUsed = BITS_PER_INT;

        // structuring completed PQ into a tree
        while (charFreqs.size() > 1) {
            TreeNode n1 = charFreqs.dequeue();
            TreeNode n2 = charFreqs.dequeue();

            if (n1.getFrequency() + n2.getFrequency() != 0) {
                // System.out.print("\njoining nodes " + (n1.getValue() == -1 ? "?" : (char)
                // n1.getValue())
                // + " and " + (n2.getValue() == -1 ? "?" : (char) n2.getValue()) + ". ");
                charFreqs.enqueue(new TreeNode(n1, -1, n2)); // add
                bitsUsed += 1;
                if (n1.getValue() != -1) {
                    // add bits from child 1
                    bitsUsed += BITS_PER_WORD;
                    // System.out.print("left child stores char " + ((char) n1.getValue()) + ". ");
                }
                if (n2.getValue() != -1) {
                    // add bits from child 1
                    bitsUsed += BITS_PER_WORD;
                    // System.out.print("right child stores char " + ((char) n2.getValue()) + ". ");
                }
            }
        }
        // System.out.println();

        // System.out.println(bitsUsed - BITS_PER_INT + " bits used to store tree");
        // printTree(charFreqs.peek(), ""); // delete
        return isTreeFormat ? bitsUsed : 0; // TODO debug: preprocess returning wrong number of bits used for standard
                                            // count format
    }

    // TODO delete; testing
    private void printTree(TreeNode n, String spaces) {
        if (n != null) {
            printTree(n.getRight(), spaces + "     ");
            System.out.println(spaces + n);
            printTree(n.getLeft(), spaces + "     ");
        }
    }

    private void findCodes(TreeNode n, HashMap<Integer, String> codes, String code) {
        if (n != null) {
            if (n.getValue() != -1) {
                // node contains a character and is a leaf
                codes.put(n.getValue(), code);
            } else {
                findCodes(n.getLeft(), codes, code + "0");
                findCodes(n.getRight(), codes, code + "1");
            }
        }
    }

    /**
     * Compresses input to output, where the same InputStream has
     * previously been pre-processed via <code>preprocessCompress</code>
     * storing state used by this call.
     * <br>
     * pre: <code>preprocessCompress</code> must be called before this method
     * 
     * @param in    is the stream being compressed (NOT a BitInputStream)
     * @param out   is bound to a file/stream to which bits are written
     *              for the compressed file (not a BitOutputStream)
     * @param force if this is true create the output file even if it is larger than
     *              the input file.
     *              If this is false do not create the output file if it is larger
     *              than the input file.
     * @return the number of bits written.
     * @throws IOException if an error occurs while reading from the input file or
     *                     writing to the output file.
     */
    public int compress(InputStream in, OutputStream out, boolean force) throws IOException {
        if (codeSequences == null) {
            throw new RuntimeException("preprocessCompress must be called before compress is called");
        }

        if (!spaceSaved && !force) {
            // compression results in a bigger file
            return -1;
        }

        // System.out.println(codeSequences);
        BitOutputStream bitsOut = new BitOutputStream(out);
        System.out.print(Integer.toBinaryString(MAGIC_NUMBER) + " " + Integer.toBinaryString(isTreeFormat ? STORE_TREE : STORE_COUNTS) + " ");
        bitsOut.writeBits(BITS_PER_INT, MAGIC_NUMBER);
        bitsOut.writeBits(BITS_PER_INT, isTreeFormat ? STORE_TREE : STORE_COUNTS);
        int bitsWritten = BITS_PER_INT * 2; // account for writing magic # and header format val

        if (isTreeFormat) {
            // TODO write data to store tree info
            bitsWritten += BITS_PER_INT;
            Queue<Integer> treeBits = new LinkedList<Integer>();
            writeTreeData(treeBits, charFreqs.peek());
            for(int i = Integer.toBinaryString(treeBits.size()).length(); i < BITS_PER_INT; i++) {
                System.out.print("0");
            }
            System.out.print(Integer.toBinaryString(treeBits.size()) + " ");
            bitsOut.writeBits(BITS_PER_INT, treeBits.size());
            bitsWritten += treeBits.size();
            // System.out.println(treeBits);
            while(!treeBits.isEmpty()) {
                System.out.print(treeBits.peek());
                bitsOut.writeBits(1, treeBits.remove());
            }
        } else {
            // write data used to construct freq array (standard count format)
            for (int character = 0; character < ALPH_SIZE; character++) {
                System.out.print(Integer.toBinaryString(freqs[character]) + " ");
                bitsOut.writeBits(BITS_PER_INT, freqs[character]);
            }
            // we do not need to write the freq for PEOF
        }

        // write content
        BitInputStream bitsIn = new BitInputStream(in);
        int character = bitsIn.readBits(BITS_PER_WORD);
        while (character != -1) {
            // System.out.println("character = " + character + ", reading " + ((char)
            // character) + " "); // delete
            String huffCode = codeSequences.get(character);
            for (int i = 0; i < huffCode.length(); i++) {
                System.out.print(huffCode.charAt(i) == '0' ? 0 : 1);
                bitsOut.writeBits(1, huffCode.charAt(i) == '0' ? 0 : 1);
                // System.out.print("" + huffCode.charAt(i));
            }
            bitsWritten += huffCode.length();
            System.out.print(" ");

            character = bitsIn.readBits(BITS_PER_WORD);
        }
        bitsIn.close();

        // write PEOF
        String peofCode = codeSequences.get(ALPH_SIZE);
        for (int i = 0; i < peofCode.length(); i++) {
            System.out.print(peofCode.charAt(i) == '0' ? 0 : 1);
            bitsOut.writeBits(1, peofCode.charAt(i) == '0' ? 0 : 1);
            // System.out.print("" + peofCode.charAt(i));
        }
        System.out.print(" ");
        bitsWritten += peofCode.length();
        while (bitsWritten % BITS_PER_WORD != 0) {
            bitsWritten++;
            System.out.print(0);
            bitsOut.writeBits(1, 0);
        }

        bitsOut.close();
        return bitsWritten;
    }

    private void writeTreeData(Queue<Integer> bits, TreeNode n) {
        if (n != null) {
            if (n.getValue() != -1) {
                // n is a leaf
                bits.add(1);
                // System.out.print("1 ");
                if (n.getValue() == PSEUDO_EOF) {
                    // n stores PEOF
                    bits.add(1);
                    for(int i = 0; i < BITS_PER_WORD; i++) {
                        bits.add(0);
                    }
                    // System.out.print("100000000 ");
                } else {
                    // n stores a character
                    bits.add(0);
                    String charAsciiAsString = Integer.toBinaryString(n.getValue());
                    for(int i = 0; i < charAsciiAsString.length(); i++) {
                        bits.add(charAsciiAsString.charAt(i) - '0');
                    }
                    // System.out.print("0" + Integer.toBinaryString(n.getValue()) + " ");
                }
            } else {
                // n is an internal node
                bits.add(0);
                // System.out.print("0 ");
                if (n.getLeft() != null) {
                    writeTreeData(bits, n.getLeft());
                }
                if (n.getRight() != null) {
                    writeTreeData(bits, n.getRight());
                }
            }
        } else { // delete
            System.out.println("n was null in writeTreeData for some reason");
        }
    }

    /**
     * Uncompress a previously compressed stream in, writing the
     * uncompressed bits/data to out.
     * 
     * @param in  is the previously compressed data (not a BitInputStream)
     * @param out is the uncompressed file/stream
     * @return the number of bits written to the uncompressed file/stream
     * @throws IOException if an error occurs while reading from the input file or
     *                     writing to the output file.
     */
    public int uncompress(InputStream in, OutputStream out) throws IOException {
        BitInputStream bitsIn = new BitInputStream(in);
        BitOutputStream bitsOut = new BitOutputStream(out);
        int bitsRead = 0;

        // reads magic number
        int magic = bitsIn.readBits(BITS_PER_INT);
        if (magic != MAGIC_NUMBER) {
            // TODO throw exception or show error msg?
            throw new IOException("Invalid magic number, file format not recognized.");
        }
        bitsRead += BITS_PER_INT;

        // Read header info test should be store counts
        int headerFormat = bitsIn.readBits(BITS_PER_INT);
        if (headerFormat != STORE_COUNTS) {
            // TODO same
            throw new IOException("Unsupported header format: " + headerFormat);
        }
        bitsRead += BITS_PER_INT;

        // read freq info
        int[] freqs = new int[ALPH_SIZE];
        for (int i = 0; i < ALPH_SIZE; i++) {
            freqs[i] = bitsIn.readBits(BITS_PER_INT);
        }

        TreeNode root = rebuildHuffmanTree(freqs);
        StringBuilder decodedContent = new StringBuilder();
        TreeNode currentNode = root;
        int bit;

        while ((bit = bitsIn.readBits(1)) != -1) {
            System.out.println(
                    "Bit read: " + bit + " | Current node: " + (currentNode != null ? currentNode.getValue() : "null"));

            // Traverse the Huffman tree based on the bit (0 or 1)
            currentNode = (bit == 0) ? currentNode.getLeft() : currentNode.getRight();

            System.out.println("After processing bit: " + bit + " | Current node: "
                    + (currentNode != null ? currentNode.getValue() : "null"));

            // If a leaf node reached there is a character
            if (currentNode != null && currentNode.isLeaf()) {
                char decodedChar = (char) currentNode.getValue();
                decodedContent.append(decodedChar);

                currentNode = root;
            }
        }

        String pseudoEofCode = codeSequences.get(PSEUDO_EOF);

        System.out.println("PSEUDO_EOF code: " + pseudoEofCode);

        bitsIn.close();
        bitsOut.close();
        return 0;
    }

    private TreeNode rebuildHuffmanTree(int[] freqs) {
        PriorityQueue314<TreeNode> pq = new PriorityQueue314<>();

        // Create leaf nodes for each character and add to priority queue
        for (int i = 0; i < ALPH_SIZE; i++) {
            if (freqs[i] > 0) {
                pq.enqueue(new TreeNode((char) i, freqs[i]));
            }
        }

        pq.enqueue(new TreeNode(PSEUDO_EOF, 1));

        System.out.println("Initial priority queue size: " + pq.size());

        while (pq.size() > 1) {
            TreeNode left = pq.dequeue();
            TreeNode right = pq.dequeue(); // TODO was poll before
            System.out.println("Queue size after poll: " + pq.size());

            TreeNode parent = new TreeNode(left, left.getFrequency() + right.getFrequency(), right);
            pq.enqueue(parent);
            System.out.println("Queue size after enqueue: " + pq.size());

            // System.out.println("Parent node created with frequency: " +
            // parent.getFrequency());
        }

        // root node
        return pq.dequeue();
    }

    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    private void showString(String s) {
        if (myViewer != null) {
            myViewer.update(s);
        }
    }
}
