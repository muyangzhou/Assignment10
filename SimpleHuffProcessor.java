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

// TODO style: no bytes (only ints)

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;

public class SimpleHuffProcessor implements IHuffProcessor {
    private IHuffViewer myViewer;
    private PriorityQueue314<TreeNode> charFreqs;
    private int[] freqs;
    private HashMap<Integer, String> codeSequences;
    private boolean spaceSaved;

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

        // structuring completed PQ into a tree
        while (charFreqs.size() > 1) {
            TreeNode n1 = charFreqs.dequeue();
            TreeNode n2 = charFreqs.dequeue();

            if (n1.getFrequency() + n2.getFrequency() != 0) {
                charFreqs.enqueue(new TreeNode(n1, -1, n2)); // add
            }
        }

        // printTree(charFreqs.peek(), ""); // delete

        // encodes characters
        findCodes(charFreqs.peek(), codeSequences, "");
        // System.out.println(codeSequences); // code sequences should be correct after
        // PQ314 has been implemented
        int bitsInCompressed = BITS_PER_INT * (2 + freqs.length);
        for (int character : codeSequences.keySet()) { // TODO traverse through freqs instead
            if (character != ALPH_SIZE) {
                bitsInCompressed += codeSequences.get(character).length() * freqs[character]; // 84
            }
        }

        bitsInCompressed += codeSequences.get(PSEUDO_EOF).length();
        bitsInCompressed += (BITS_PER_WORD - (bitsInCompressed % BITS_PER_WORD)) % BITS_PER_WORD;

        showString((bitsInOriginal - bitsInCompressed) + " bits saved.");
        System.out.println(bitsInOriginal + " bits in original, " + bitsInCompressed + " bits in compressed");
        System.out.println((bitsInOriginal - bitsInCompressed) + " bits saved detected in SimpleHuffProcessor");
        spaceSaved = bitsInOriginal > bitsInCompressed;
        // myViewer.update("Still not working");
        return bitsInOriginal - bitsInCompressed;
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
        if (!spaceSaved && !force) {
            // compression results in a bigger file
            return -1;
        }

        // System.out.println(codeSequences);
        BitOutputStream bitsOut = new BitOutputStream(out);
        bitsOut.writeBits(BITS_PER_INT, MAGIC_NUMBER);
        bitsOut.writeBits(BITS_PER_INT, STORE_COUNTS);
        int bitsWritten = BITS_PER_INT * 2;

        // write data used to construct freq array (standard count format)
        for (int character = 0; character < ALPH_SIZE; character++) {
            bitsOut.writeBits(BITS_PER_INT, freqs[character]);
        }

        // write content
        BitInputStream bitsIn = new BitInputStream(in);
        int character = bitsIn.readBits(BITS_PER_WORD);
        while (character != -1) {
            // System.out.println("character = " + character + ", reading " + ((char)
            // character) + " "); // delete
            String huffCode = codeSequences.get(character);
            for (int i = 0; i < huffCode.length(); i++) {
                bitsOut.writeBits(1, huffCode.charAt(i) == '0' ? 0 : 1);
                // System.out.print("" + huffCode.charAt(i));
            }
            bitsWritten += huffCode.length();
            // System.out.print(" ");

            character = bitsIn.readBits(BITS_PER_WORD);
        }
        bitsIn.close();

        // write PEOF
        String peofCode = codeSequences.get(ALPH_SIZE);
        for (int i = 0; i < peofCode.length(); i++) {
            bitsOut.writeBits(1, peofCode.charAt(i) == '0' ? 0 : 1);
            // System.out.print("" + peofCode.charAt(i));
        }
        // System.out.println();
        bitsWritten += peofCode.length();
        while (bitsWritten % BITS_PER_WORD != 0) {
            bitsWritten++;
            bitsOut.writeBits(1, 0);
        }

        bitsOut.close();
        return bitsWritten;
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
        throw new IOException("uncompress not implemented");
        // return 0;
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
