# SBITE
Simplistic BIdirectional Templating [for Excel]

SBITE is inspired from Excel templating libraries such as JETT or JXLS, but simplify them drastically in order to allow generating back the data from a filled excel file obeying the template structure.
If you're looking for fancy scripting language and advanced dynamic formatting in your template, this is not the library you're looking for.

SBITE is:
- Bidirectional: Template XSLX + Data = XLSX filled with Data, and Template XLSX + XLSX filled with Data = Data
- Fast & Memory efficient: Whenever data is involved, Streaming APIs are used (SXSSF).
- Secure & end-user ready: There's no scripting language allowed in the template. This limits flexibility, but also makes it impossible to write malicious code in the template.




