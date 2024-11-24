/*  Student information for assignment:
 *
 *  On our honor, Muyang Zhou and Olivia Wang, this programming assignment is ou4 own work
 *  and we have not provided this code to any other student.
 *
 *  Number of slip days used: 2
 *
 *  Student 1 (Student whose Canvas account is being used)
 *  UTEID: mz9939
 *  email address: m.zhou@utexas.edu
 *  Grader name: Diego
 *
 *  Student 2
 *  UTEID: oyw74
 *  email address: oliviawang@utexas.edu
 *
 */

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Queue;
import java.util.LinkedList;

/**
 * Processes compressed and original files
 * Compresses and reverts .hf files using Huffman Compression algorithms
 */
public class SimpleHuffProcessor implements IHuffProcessor {
    private IHuffViewer myViewer;
    private PriorityQueue314<TreeNode> charFreqs;
    private int[] freqs;
    private HashMap<Integer, String> codeSequences;
    private boolean spaceSaved;
    private boolean isTreeFormat;

    /**
     * Default constructor for SimpleHuffProcessor
     */
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
            showString("Parameter headerFormat must equal IHuffConstants.STORE_COUNTS or "
                    + "IHuffConstants.STORE_TREE");
            return -1;
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
        bitsInCompressed += buildTree(freqs);
        findCodes(charFreqs.peek(), codeSequences, "");
        bitsInCompressed += isTreeFormat ? BITS_PER_INT * freqs.length : 0;

        // the actual data
        for (int character : codeSequences.keySet()) {
            if (character != ALPH_SIZE) {
                bitsInCompressed += codeSequences.get(character).length() * freqs[character];
            }
        }

        bitsInCompressed += codeSequences.get(PSEUDO_EOF).length();

        showString((bitsInOriginal - bitsInCompressed) + " bits saved.");
        spaceSaved = bitsInOriginal > bitsInCompressed;
        return bitsInOriginal - bitsInCompressed;
    }

    /**
     * Builds the PriorityQueue314 of characters and constructs them into a tree
     * 
     * @param freqs an array of ints storing the frequency of each ascii value in
     *              the file
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

        int bitsUsed = BITS_PER_INT;

        // structuring completed PQ into a tree
        while (charFreqs.size() > 1) {
            TreeNode n1 = charFreqs.dequeue();
            TreeNode n2 = charFreqs.dequeue();

            if (n1.getFrequency() + n2.getFrequency() != 0) {
                charFreqs.enqueue(new TreeNode(n1, -1, n2)); // add
                bitsUsed += 1;
                if (n1.getValue() != -1) {
                    // add bits from child 1
                    bitsUsed += BITS_PER_WORD;
                }
                if (n2.getValue() != -1) {
                    // add bits from child 1
                    bitsUsed += BITS_PER_WORD;
                }
            }
        }
        return isTreeFormat ? bitsUsed : 0;
    }

    /**
     * Constructs Huffman codes of each character and stores in HashMap
     * 
     * @param n     current TreeNode
     * @param codes HashMap to add codes to
     * @param code  Huffman code constructed
     */
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
            throw new RuntimeException("preprocessCompress must be called first");
        }

        if (!spaceSaved && !force) {
            // compression results in a bigger file
            return -1;
        }

        BitOutputStream bitsOut = new BitOutputStream(out);
        bitsOut.writeBits(BITS_PER_INT, MAGIC_NUMBER);
        bitsOut.writeBits(BITS_PER_INT, isTreeFormat ? STORE_TREE : STORE_COUNTS);
        int bitsWritten = BITS_PER_INT * 2; // account for writing magic # and header format val

        bitsWritten += writeHeader(bitsOut);

        // write content
        BitInputStream bitsIn = new BitInputStream(in);
        int character = bitsIn.readBits(BITS_PER_WORD);
        while (character != -1) {
            String huffCode = codeSequences.get(character);
            for (int i = 0; i < huffCode.length(); i++) {
                bitsOut.writeBits(1, huffCode.charAt(i) == '0' ? 0 : 1);
            }
            bitsWritten += huffCode.length();

            character = bitsIn.readBits(BITS_PER_WORD);
        }
        bitsIn.close();

        bitsWritten += writePeof(bitsOut);
        bitsOut.close();
        return bitsWritten;
    }

    /**
     * Writes PEOF and calculates the number of bits written
     * 
     * @param bitsOut BitOutputStream used to write the compressed file
     * @return the number of bits written by the PEOF
     */
    private int writePeof(BitOutputStream bitsOut) {
        int bitsWritten = 0;
        String peofCode = codeSequences.get(ALPH_SIZE);
        for (int i = 0; i < peofCode.length(); i++) {
            bitsOut.writeBits(1, peofCode.charAt(i) == '0' ? 0 : 1);
        }
        bitsWritten += peofCode.length();
        while (bitsWritten % BITS_PER_WORD != 0) {
            bitsWritten++;
            bitsOut.writeBits(1, 0);
        }

        return bitsWritten;
    }

    /**
     * Writes header and calculates the number of bits written
     * 
     * @param bitsOut BitOutputStream used to write the compressed file
     * @return the number of bits written by the header
     */
    private int writeHeader(BitOutputStream bitsOut) {
        int bitsWritten = 0;
        if (isTreeFormat) {
            bitsWritten += BITS_PER_INT;
            Queue<Integer> treeBits = new LinkedList<Integer>();
            writeTreeData(treeBits, charFreqs.peek());
            bitsOut.writeBits(BITS_PER_INT, treeBits.size());
            bitsWritten += treeBits.size();
            while (!treeBits.isEmpty()) {
                bitsOut.writeBits(1, treeBits.remove());
            }
        } else {
            // write data used to construct freq array (standard count format)
            for (int character = 0; character < ALPH_SIZE; character++) {
                bitsOut.writeBits(BITS_PER_INT, freqs[character]);
            }
            // we do not need to write the freq for PEOF
        }
        return bitsWritten;
    }

    /**
     * Recursively finds the bits to write ino the compressed file
     * 
     * @param bits a Queue that stores the bits to be written into the compressed
     *             file
     * @param n    the current node
     */
    private void writeTreeData(Queue<Integer> bits, TreeNode n) {
        if (n != null) {
            if (n.getValue() != -1) {
                // n is a leaf
                bits.add(1);
                if (n.getValue() == PSEUDO_EOF) {
                    // n stores PEOF
                    bits.add(1);
                    for (int i = 0; i < BITS_PER_WORD; i++) {
                        bits.add(0);
                    }
                } else {
                    // n stores a character
                    bits.add(0);
                    String charAsciiAsString = Integer.toBinaryString(n.getValue());
                    for (int i = 0; i < charAsciiAsString.length(); i++) {
                        bits.add(charAsciiAsString.charAt(i) - '0');
                    }
                }
            } else {
                // n is an internal node
                bits.add(0);
                if (n.getLeft() != null) {
                    writeTreeData(bits, n.getLeft());
                }
                if (n.getRight() != null) {
                    writeTreeData(bits, n.getRight());
                }
            }
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
        int bitsRead = BITS_PER_INT * 2;

        // reads magic number
        int magic = bitsIn.readBits(BITS_PER_INT);
        if (magic != MAGIC_NUMBER) {
            showString("Invalid magic number, file format not recognized.");
            bitsIn.close();
            return -1;
        }

        // read header info
        int headerFormat = bitsIn.readBits(BITS_PER_INT);
        TreeNode root;
        if (headerFormat == STORE_TREE) {
            root = readTree(bitsIn);
        } else if (headerFormat == STORE_COUNTS) {
            int[] frequencies = new int[ALPH_SIZE];
            for (int i = 0; i < ALPH_SIZE; i++) {
                frequencies[i] = bitsIn.readBits(BITS_PER_INT);
            }
            buildTree(frequencies);
            root = charFreqs.peek();
        } else {
            // invalid header format
            showString("Invalid header format");
            bitsIn.close();
            return -1;
        }

        bitsRead += readContent(bitsIn, out, root);
        bitsIn.close();
        return bitsRead;
    }

    /**
     * 
     * @param bitsIn BitInputStream used to read compressed file
     * @param out    OutputStream used to write original file
     * @param root   Root node of Huffman Tree
     * @return number of bits read in this method
     * @throws IOException if there is an error reading the file
     */
    private int readContent(BitInputStream bitsIn, OutputStream out, TreeNode root)
            throws IOException {
        BitOutputStream bitsOut = new BitOutputStream(out);
        TreeNode current = root;
        int bit = bitsIn.readBits(1);
        int bitsRead = 0;
        while (bit != -1) {
            current = (bit == 0) ? current.getLeft() : current.getRight();

            if (current.isLeaf()) {
                if (current.getValue() == PSEUDO_EOF) {
                    break;
                }
                bitsOut.writeBits(BITS_PER_WORD, current.getValue());
                bitsRead += BITS_PER_WORD;
                current = root;
            }

            bit = bitsIn.readBits(1);
        }
        bitsOut.close();

        return bitsRead;
    }

    /**
     * Reads a Huffman tree from the input stream
     *
     * @param bitIn the input stream
     * @return the root of Huffman tree.
     */
    private TreeNode readTree(BitInputStream bitIn) throws IOException {
        int bit = bitIn.readBits(1);
        if (bit == 0) {
            // internal node
            TreeNode left = readTree(bitIn);
            TreeNode right = readTree(bitIn);
            return new TreeNode(left, -1, right);
        } else {
            // leaf node
            int isPseudoEOF = bitIn.readBits(1);
            if (isPseudoEOF == 1) {
                return new TreeNode(PSEUDO_EOF, 1);
            } else {
                int value = bitIn.readBits(BITS_PER_WORD);
                return new TreeNode(value, 1);
            }
        }
    }

    /**
     * Sets myViewer to parameter
     */
    public void setViewer(IHuffViewer viewer) {
        myViewer = viewer;
    }

    /**
     * Displays a string in the panel
     * 
     * @param s string to display
     */
    private void showString(String s) {
        if (myViewer != null) {
            myViewer.update(s);
        }
    }
}
