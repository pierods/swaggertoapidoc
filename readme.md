
# A Swagger to Apidoc converter.

 Usage:
 
 java -jar ramltoapidocconverter-1.0.jar -raml <uri of raml (file)> [-apidoc <name of apidoc output file>]
 
 if output file is omitted, it will write to stdout
 
 or
 
 java -jar swtoapidoc-1.0.jar -swagger <uri of swagger yaml> -appname <apidoc app key> -apidoc <name of apidoc output file> 

or 
 java -jar swtoapidoc-1.0.jar -swagger <uri of swagger yaml> -appname <apidoc app key>

will output to stdout.

 java -jar swtoapidoc-1.0.jar -swagger <uri of swagger yaml> -version 

will output version of swagger file to stdout.
 
 Possible improvements:
 - more test coverage
 - support for enumerations...
