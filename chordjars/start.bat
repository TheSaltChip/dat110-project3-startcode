sh chord.sh
timeout 4
start cmd.exe @cmd /k java -jar process1.jar
timeout 2
start cmd.exe @cmd /k java -jar process2.jar
timeout 2
start cmd.exe @cmd /k java -jar process3.jar
timeout 2
start cmd.exe @cmd /k java -jar process4.jar
timeout 2
start cmd.exe @cmd /k java -jar process5.jar