package ru.ifmo.genetics.structures.debriujn;

import org.apache.commons.lang.mutable.MutableInt;
import org.junit.Test;
import ru.ifmo.genetics.dna.*;
import ru.ifmo.genetics.dna.kmers.BigKmer;
import ru.ifmo.genetics.dna.kmers.ImmutableBigKmer;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class WightedDeBruijnGraphTest {
    Random r;
    {
        long seed = this.hashCode() + System.nanoTime();
        r = new Random(seed);
        System.err.println("Seed = " + seed);
    }

    @Test
    public void testAddEdge() throws IOException {
        int k = 15;
        WeightedDeBruijnGraph g = new WeightedDeBruijnGraph(k, 1000, 1);

        LightDna testDna = new Dna("TATGTTCAGATAATGCCCGATGACTTTGTCATGCAGCTCCAC");
        LightDna testDna2 = new Dna("GATGCTGAAAAGAGTAGTAATTGCTGGTAATGACTCCAACTTA");

        HashMap<BigKmer, MutableInt> edges = new HashMap<BigKmer, MutableInt>();

        for (int i = 0; i + k < testDna.length(); ++i) {
            ImmutableBigKmer kmer = new ImmutableBigKmer(new DnaView(testDna, i, i + k + 1));
            if (!edges.containsKey(kmer)) {
                edges.put(kmer, new MutableInt());
            }

            int weight = 1 + r.nextInt(1024);
//            System.out.println("kmer: " + kmer + ", weight: " + weight);
            MutableInt totalWeight = edges.get(kmer);

            totalWeight.add(weight);

            g.addEdge(kmer, weight);

        }

        assertTrue(g.containsEdges(testDna));
        assertFalse(g.containsEdges(testDna2));

        for (Map.Entry<BigKmer, MutableInt> entry: edges.entrySet()) {
            assertEquals("kmer: " + entry.getKey(), entry.getValue().intValue(), g.getWeight(entry.getKey()));
        }


    }
}

