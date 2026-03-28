

# Reports tab


## Licensing
- Feature flagged as experimental.  Reports tab now appears when experimental features are enabled.
 Reports tab is hidden when experimental features are disabled.
 Reports tab shows locked placeholder for non-premium users when enabled

## Layout (premium intellij module)

7. Add a reports tab, which shows a grid (2 columns wide) with cards for different report types. Each card has a button to generate the PDF report based on the scan data. 

1. Reports tab shows a list of links to open PDF reports which have been previously generated.


## Actions (premium intellij module)
- Clicking on a reports 'generate' button generates the PDF report asynchronously in the background
- While a report is being generated, a progress bar is shown.
- When report generation is complete, the UI prompts to either save it or open it.
- Choosing to save it shows a folder selection dialog.
- Choosing to open it opens the generated PDF in the system's default PDF application.
- When a report is generated, a link is added to the list of generated reports.



## Logic  (premium core module)
- Use the modern PDFBox library which can easily create basic PDFs with intiuitive formatting/layouts, ideally based on a plain-text template which is easy to maintain like JSON, YAML, or Markdown.

8. To begin with, we will just have a single PDF report, which will display the following headings with scan data under each heading: 
- dependency tree with only organisation dependencies
- maven dependency list
- maven dependency tree 
- advanced scan results (heading per advanced scan type)

2. Add a footer at the end of every PDF with the same links from the support tab




