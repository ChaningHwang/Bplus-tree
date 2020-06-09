# Bplus-tree

Java implementation of B+ tree.

## Input Format:
The first line in the input file Initialize(m) means creating a B+ tree with the order m (note: m may be different depending on input file). Each of the remaining lines specifies a B+ tree operation. The following is an example of an input file:  
Initialize(3)  
Insert(21, 0.3534)  
Insert(108, 31.907)  
Insert(56089, 3.26)  
Insert(234, 121.56)  
Insert(4325, -109.23)  
Delete (108)  
Search(234)  
Insert(102, 39.56)  
Insert(65, -3.95)  
Delete (102)  
Delete (21)  
Insert(106, -3.91)  
Insert(23, 3.55)  
Search(23, 99)  
Insert(32, 0.02)  
Insert(220, 3.55)  
Delete (234)  
Search(65)  
You can use integer as the type of the key and float/double as the type of the value.

## Output Format:
For Initialize, Insert and Delete query you should not produce any output.  
For a Search query you should output the results on a single line using commas to separate values. The output for each search query should be on a new line. All output should go to a file named “output_file.txt”. If a search query does not return anything you should output “Null”.  
The following is the output file for the above input file:  
121.56  
3.55,-3.95  
-3.95  
