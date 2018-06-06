package ru.ifmo.genetics.tools.microassembly;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.util.LineReader;
import ru.ifmo.genetics.distributed.io.writable.DnaWritable;
import ru.ifmo.genetics.dna.Dna;
import ru.ifmo.genetics.dna.DnaView;
import ru.ifmo.genetics.dna.LightDna;
import ru.ifmo.genetics.io.readers.FastaRecordReader;

import java.io.BufferedInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.*;

public class JoinContigs {
    public static String rcId(String id) {
        if (id.endsWith("rc")) {
            return id.substring(0, id.length() - 2);
        } else {
            return id + "rc";
        }
    }
    
    private static class Edge {
        String to;
        int distance;
        LightDna sequence;

        private Edge(String to, int distance, LightDna sequence) {
            this.to = to;
            this.distance = distance;
            this.sequence = sequence;
        }
    }
    
    public static void main(String[] args) throws IOException {
        
        String contgsFile = args[0];
        String holesFile = args[1];

        FastaRecordReader reader = new FastaRecordReader(new BufferedInputStream(new FileInputStream(contgsFile)));
        DnaWritable dna = new DnaWritable();
        Text id = new Text();

        Map<String, LightDna> contigs = new TreeMap<String, LightDna>();
         
        while (reader.next(id, dna)) {
            Dna contig = new Dna(dna);
            contigs.put(id.toString(), contig);


            contigs.put(id.toString() + "rc", contig.reverseComplement());
        }
        
        Text line = new Text();

        LineReader holesReader = new LineReader(new BufferedInputStream(new FileInputStream(holesFile)));
        
        HashMap<String, Edge> edges = new HashMap<String, Edge>();
        
        Dna emptyDna = new Dna();
        
        
        while (holesReader.readLine(line) != 0) {
            String s = line.toString();
            String[] parts = s.split("\t");
            String[] holeParts = parts[0].split(" ");
            String[] fillerParts = parts[1].split(" ");
            int dist = Integer.parseInt(fillerParts[0]);
            if (dist < -300) {
                System.err.println("ignoring: " + line);
                continue;
            }
            Dna fillerSequence = emptyDna;

            if (fillerParts.length == 2) {
                fillerSequence = new Dna(fillerParts[1]);
            }
            
            String from = holeParts[0];
            String to = holeParts[2];
            
            assert !edges.containsKey(from);
            edges.put(from, new Edge(to, dist, fillerSequence));
                    
            assert !edges.containsKey(rcId(to));
            edges.put(rcId(to), new Edge(rcId(from), dist, fillerSequence.reverseComplement()));
        }
        
        
        HashSet<String> used = new HashSet<String>(contigs.size());
        
        for (Map.Entry<String, LightDna> entry: contigs.entrySet()) {
            String curId = entry.getKey();
            if (used.contains(curId)) {
                continue;
            }

            while (edges.containsKey(rcId(curId))) {
                curId = rcId(edges.get(rcId(curId)).to);
                assert !used.contains(curId);
                if (curId.equals(entry.getKey())) {
                    break;
                }
            }

            ArrayList<String> markUsed = new ArrayList<String>();
            
            String startId = curId;
            
            System.out.println(">" + curId);
            System.out.print(contigs.get(curId));
            System.err.println(startId + " " + 0 + " " + startId);
            markUsed.add(curId);
            int shift = contigs.get(curId).length();
            while (edges.containsKey(curId)) {
                Edge e = edges.get(curId);
                System.out.print(e.sequence);
                System.err.println("<" + e.sequence.length() + ">");
                shift += e.sequence.length();
                if (e.to.equals(startId)) {
                    break;
                }
                LightDna toContig = contigs.get(e.to);
                DnaView view = new DnaView(toContig, Math.max(-e.distance, 0), toContig.length());
                System.out.print(view);
                System.err.println(startId + " " + shift + " " + e.to);
                shift += view.length();
                curId = e.to;
                assert !used.contains(curId);
                markUsed.add(curId);
            }
            System.out.println();
            for (String s: markUsed) {
                used.add(s);
                used.add(rcId(s));
            }
        }

        for (Map.Entry<String, LightDna> entry: contigs.entrySet()) {
            assert used.contains(entry.getKey());
        }
    }
}
