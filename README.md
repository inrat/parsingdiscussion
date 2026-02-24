# parsingdiscussion
A project made with Java that parses the source code of a discussion forum into a xlsx-file. 

Version 1 was created in 2022 for a person that needed to analyze the discussion data from ylilauta.org.
The project is pretty messy compared using API, but I decided at the time to execute it with my existing knowledge. 

The program was updated in 2026, since changes to source file rendered the program that only parsed the source file useless. 
The new version (ylis_data.py) uses Playwright to load threads in a headless browser and calls the site's API to retrieve all posts. The output format remains the same: an Excel file mapping each poster's user ID to the user ID they replied to.

Written with the help of Claude (Anthropic).
