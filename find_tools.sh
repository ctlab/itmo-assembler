#!/bin/sh

chmod a+x full_class_path.sh
java -ea -cp "$(./full_class_path.sh)" ru.ifmo.genetics.ToolsScanner "$@"
