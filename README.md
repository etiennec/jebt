# SBET
Simplistic Bidirectional Excel Templating

SBET is inspired from Excel templating libraries such as JETT or JXLS, but simplify them drastically in order to allow generating back the data from a filled excel file obeying the template structure.
t sIf you're looking for fancy scripting language and advanced dynamic formatting in your template, this is not the library you're looking for.

SBET is:
- Bidirectional: Template XSLX + Data = XLSX filled with Data, and Template XLSX + XLSX filled with Data = Data
- Fast & Memory efficient: Whenever data is involved, Streaming APIs are used (SXSSF).
- Secure & end-user ready: There's no scripting language allowed in the template. This limits flexibility, but also makes it impossible to write malicious code in the template.
- Data output agnostic, as long as you can convert data to a Java bean. Support is planned for Java Pojo, JSon, and Database table



