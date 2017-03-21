Execution Steps

To execute the program, please follow the below mentioned steps.

Please use only Linux/Unix system to run the program

1) Make sure the Folder "lib" & "src" are present in the same directory as the mainSection.java file.

2) Place the .txt file to be processed in the same folder.

3) Compile the code using the below command in the terminal (Make sure you are inside the same directory as the java file).

		 javac -cp ".:./lib/*" -d "./src" mainSection.java

4) To run the program use the below command in the terminal inside the same folders.
   We need to provide command line arguments for the code to be executed. 
   			mainSection <K Value> <InputFile>

		 java -cp ".:./lib/*:./src"  mainSection 15 p1data.txt

5) The output files will be generated in the same folder where the program has been executed.

