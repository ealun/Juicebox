/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2011-2017 Broad Institute, Aiden Lab
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 *  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 *  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 *  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 *  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 *  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 *  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 *  THE SOFTWARE.
 */

package juicebox.assembly;

import javafx.util.Pair;
import juicebox.gui.SuperAdapter;
import juicebox.track.feature.Feature2D;
import juicebox.track.feature.Feature2DList;

import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

/**
 * Created by ranganmostofa on 6/29/17.
 */
public class AssemblyFileImporter {
    private SuperAdapter superAdapter;
    private String chromosomeName = "assembly";
    private String cpropsFilePath;
    private String asmFilePath;
    private List<Pair<String, Integer>> contigProperties;
    private List<List<Integer>> scaffoldProperties;
    private Feature2DList contigs;
    private Feature2DList scaffolds;

    public AssemblyFileImporter(String cpropsFilePath, String asmFilePath) {
        this.cpropsFilePath = cpropsFilePath;
        this.asmFilePath = asmFilePath;
        contigProperties = new ArrayList<>();
        scaffoldProperties = new ArrayList<>();
        contigs = new Feature2DList();
        scaffolds = new Feature2DList();
    }

    public void readFiles() {
        try {
            parseCpropsFile();
            parseAsmFile();
            populateContigsAndScaffolds();
        } catch (IOException exception) {
            System.err.println("Error reading files!");
        }
    }

    private void parseCpropsFile() throws IOException {
        if (validateCpropsFile()) {
            List<String> rawFileData = readFile(cpropsFilePath);

            for (String row : rawFileData) {
                String[] splitRow = row.split(" ");
                // splitRow[0] -> Name, splitRow[2] -> length
                Pair<String, Integer> currentPair = new Pair<>(splitRow[0], Integer.parseInt(splitRow[2]));
                contigProperties.add(currentPair);
            }
        }
    }

    private boolean validateCpropsFile() {
        return getCpropsFilePath().endsWith(FILE_EXTENSIONS.CPROPS.toString());
    }

    private void parseAsmFile() throws IOException {
        if (validateAsmFile()) {
            List<String> rawFileData = readFile(asmFilePath);

            for (String row : rawFileData) {
                List<Integer> currentContigIndices = new ArrayList<>();
                for (String index : row.split(" ")) {
                    currentContigIndices.add(Integer.parseInt(index));
                }
                scaffoldProperties.add(currentContigIndices);
            }
        }
    }

    private boolean validateAsmFile() {
        return getAsmFilePath().endsWith(FILE_EXTENSIONS.ASM.toString());
    }

    private void populateContigsAndScaffolds() {
        Integer contigStartPos = 0;
        Integer scaffoldStartPos = 0;
        Integer scaffoldLength = 0;
        for (List<Integer> row : scaffoldProperties) {
            for (Integer contigIndex : row) {
                String contigName = contigProperties.get(Math.abs(contigIndex)).getKey();
                Integer contigLength = contigProperties.get(Math.abs(contigIndex)).getValue();

                Feature2D contig = new Feature2D(Feature2D.FeatureType.CONTIG, chromosomeName, contigStartPos, contigStartPos,
                        chromosomeName, contigStartPos + contigLength, contigStartPos + contigLength,
                        new Color(0, 255, 0), new HashMap<String, String>());
                contigs.add(1, 1, contig);

                contigStartPos += contigLength;
                scaffoldLength += contigLength;
            }
            Feature2D scaffold = new Feature2D(Feature2D.FeatureType.SCAFFOLD, chromosomeName, scaffoldStartPos, scaffoldStartPos,
                    chromosomeName, scaffoldStartPos + scaffoldLength, scaffoldStartPos + scaffoldLength,
                    new Color(0, 0, 255), new HashMap<String, String>());
            scaffolds.add(1, 1, scaffold);
        }
    }

    private boolean getIsInverted(Integer contigIndex) {
        return contigIndex < 0 ? Boolean.TRUE : Boolean.FALSE;
    }

    public void buildContigAttributes(String contigName, Integer contigLength) {
        Map<String, String> featureAttributes = new HashMap<>();
//        featureAttributes.put("Scaffold_ID);
    }

    private List<String> readFile(String filePath) throws IOException {
        List<String> fileData = new ArrayList<>();

        File file = new File(filePath);
        Scanner scanner = new Scanner(file);

        while (scanner.hasNext()) {
            fileData.add(scanner.next());
        }

        return fileData;
    }

    private String getCpropsFilePath() {
        return this.cpropsFilePath;
    }

    private void setCpropsFilePath(String cpropsFilePath) {
        this.cpropsFilePath = cpropsFilePath;
    }

    private String getAsmFilePath() {
        return this.asmFilePath;
    }

    private void setAsmFilePath(String asmFilePath) {
        this.asmFilePath = asmFilePath;
    }

    public Feature2DList getContigs() {
        return this.contigs;
    }

    public Feature2DList getScaffolds() {
        return this.scaffolds;
    }

    private enum FILE_EXTENSIONS {
        CPROPS("cprops"),
        ASM("asm");

        private final String extension;

        FILE_EXTENSIONS(String extension) {
            this.extension = extension;
        }

        public boolean equals(String otherExtension) {
            return this.extension.equals(otherExtension);
        }

        public String toString() {
            return this.extension;
        }
    }
}