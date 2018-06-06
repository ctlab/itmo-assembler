package ru.ifmo.genetics.tools.microassembly;

import ru.ifmo.genetics.distributed.contigsJoining.types.Filler;
import ru.ifmo.genetics.distributed.contigsJoining.types.Hole;

public class FilledHole {
    public Hole hole = new Hole();
    public Filler filler = new Filler();
    public FilledHole(String line) {
        String[] parts = line.split("\t");
        hole.parse(parts[0]);
        filler.parse(parts[1]);
    }
    

}
