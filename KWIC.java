package kwic;

//Librerias de apache para leer archivos pdf
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.apache.pdfbox.text.TextPosition;

import javax.swing.*;
import java.io.*;
import java.util.*;


public class KWIC {

    //Sección de datos compartidos
    private static List<String> inputSentences = new ArrayList<>(); //Contendrá las líneas de entrada
    private static List<String> kwicIndex = new ArrayList<>(); //Contendrá la lista de lineas en orden circular y ordenadas

    //Programa principal desde el cual se mandan llamar las diversas subrutinas
    public static void main(String[] args) {
        //lecturaPalabrasAIgnorar();
        inputReading();
        circularShift();
        alphabetize();
        writeToOutput();
    }

    //Lee un archivo de entrada y almacena las líneas en una Lista
    private static void inputReading() {
        String archivo;
        JFileChooser jFC = new JFileChooser();
        jFC.setDialogTitle("KWIC - Seleccione el archivo de datos deseado");
        jFC.setCurrentDirectory(new File("src"));
        int res = jFC.showOpenDialog(null);
        if (res == JFileChooser.APPROVE_OPTION) {
            archivo = jFC.getSelectedFile().getPath();
        } else {
            archivo = "src/input_iMDB.txt";
        }
        try {
            File file = new File(archivo);
            Scanner scanner = new Scanner(file);
            while (scanner.hasNextLine()) {
                String sentencia = scanner.nextLine();
                if (sentencia.isEmpty()) {
                    break;
                }
                inputSentences.add(sentencia);
            }
            scanner.close();
        } catch (FileNotFoundException e) {
            System.out.println("El archivo no existe");
        }
    }

    //Genera las sentencias circulares
    private static void circularShift() {
        for (String inputSentence : inputSentences) {
            List<String> words = splitSentenceIntoWords(inputSentence);
            for (int i = 0; i < words.size(); i++) {
                String word = words.get(i);
                String shiftedSentence = formSentenceFromWords(words, i);
                kwicIndex.add(shiftedSentence);
            }
        }
    }

    //Ordena la lista de sentencias
    private static void alphabetize() {
        Collections.sort(kwicIndex, String.CASE_INSENSITIVE_ORDER);
    }

    //Envia la lista de sentencias a un archivo de salida
    private static void writeToOutput() {
        FileWriter salida = null;
        String archivo;
        JFileChooser jFC = new JFileChooser();
        jFC.setDialogTitle("KWIC - Seleccione el archivo de salida");
        jFC.setCurrentDirectory(new File("src"));
        int res = jFC.showSaveDialog(null);
        if (res == JFileChooser.APPROVE_OPTION) {
            archivo = jFC.getSelectedFile().getPath();
        } else {
            archivo = "src/output.txt";
        }
        try {
            salida = new FileWriter(archivo);
            PrintWriter bfw = new PrintWriter(salida);
            System.out.println("Índice-KWIC:");
            for (String sentence : kwicIndex) {
                bfw.println(sentence);
                System.out.println(sentence);
            }
            bfw.close();
            System.out.println("Se ha creado satisfactoriamente el archivo de texto");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // El siguiente método separa una sentencia en las palabras que lo componen
    private static List<String> splitSentenceIntoWords(String sentencia) {
        String[] palabras = sentencia.split("\\s+");
        return new ArrayList<>(Arrays.asList(palabras));
    }

    // El siguiente método forma una sentencia a partir de una lista de palabras
    private static String formSentenceFromWords(List<String> words, int offset) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < words.size(); i++) {
            int idx = (i + offset) % words.size();
            String word = words.get(idx);
            sb.append(word).append(" ");
        }
        return sb.toString().trim();
    }


    public static class PDFBuscadorPalabras {
        private String pathPdf;

        public PDFBuscadorPalabras(String direccionDelPDF) {
            this.pathPdf = direccionDelPDF;
        }

        private List<TextPositionSequence> findSubwords(PDDocument documentoPDF, int numPagina, String searchTerm) throws IOException {

            final List<TextPositionSequence> hits = new ArrayList<>();

            PDFTextStripper stripper = new PDFTextStripper() {
                        @Override
                        protected void writeString(String texto, List<TextPosition> posicionesDelTexto)
                                throws IOException {
                            TextPositionSequence palabra = new TextPositionSequence(posicionesDelTexto);
                            String string = palabra.toString();

                            int fromIndex = 0;
                            int index;
                            while ((index = string.indexOf(searchTerm, fromIndex)) > -1) {
                                hits.add(palabra.subSequence(index, index + searchTerm.length()));
                                fromIndex = index + 1;
                            }
                            super.writeString(texto, posicionesDelTexto);
                        }
                    };

            stripper.setSortByPosition(true);
            stripper.setStartPage(0);
            stripper.setEndPage(numPagina);
            stripper.getText(documentoPDF);
            return hits;
        }

        private List<String> encontrarPalabraPorNumPagina(String path, List<String> palabras) {
            List<String> wordsResult = new ArrayList<>();
            if (palabras != null && !palabras.isEmpty()) {
                try {
                    PDDocument document = PDDocument.load(new File(path));

                    int pos;
                    StringBuilder format;
                    for (int i = 0; i < document.getNumberOfPages(); i++) {
                        pos = 0;
                        for (String item : palabras) {

                            item = item.replaceAll("\n", "");
                            boolean isUpdate = wordsResult.size() - 1 > 0 && pos <= (wordsResult.size() - 1);
                            format = new StringBuilder((isUpdate) ? wordsResult.get(pos) : "");

                            if (i == 0) {
                                format.append("Palabra:");
                                format.append(item);
                                format.append(", Paginas:");
                            }

                            List<TextPositionSequence> textPositionSequences = findSubwords(document, i, item);
                            if (!textPositionSequences.isEmpty()) {
                                format.append(i);
                                format.append(" ");
                            }

                            if (isUpdate) {
                                wordsResult.set(pos, format.toString());
                            } else {
                                wordsResult.add(pos, format.toString());
                            }
                            pos++;
                        }
                    }

                    wordsResult.addAll(palabras);

                } catch (IOException e) {
                    System.out.println(e.toString());
                }
            }
            return wordsResult;
        }
    }

    public static class TextPositionSequence implements CharSequence {
        private final List<TextPosition> textPositions;
        private final int start, end;

  public TextPositionSequence(List<TextPosition> textPositions) {
            this(textPositions, 0, textPositions.size());
        }

  public TextPositionSequence(List<TextPosition> textPositions, int start, int end) {
            this.textPositions = textPositions;
            this.start = start;
            this.end = end;
        }

        @Override
        public int length() { return end - start; }

        @Override
        public char charAt(int index) {
            TextPosition textPosition = textPositionAt(index);
            String text = textPosition.getUnicode();
            return text.charAt(0);
        }

        @Override
        public TextPositionSequence subSequence(int start, int end) {
            return new TextPositionSequence(textPositions, this.start + start, this.start + end);
        }

        @Override
        public String toString() {
            StringBuilder builder = new StringBuilder(length());
            for (int i = 0; i < length(); i++) {
                builder.append(charAt(i));
            }
            return builder.toString();
        }

        public TextPosition textPositionAt(int index) {
            return textPositions.get(start + index);
        }
    }
}

