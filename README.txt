Cowo
First released 25 November 2012 by Clement Levallois
www.clementlevallois.net
twitter: @seinecle

What does Cowo do?
Cowo takes a file containing several lines of text and creates a map of the terms contained in these lines (with VosViewer, www.vosviewer.com)

What kind of "map of terms"?
=> http://www.clementlevallois.net/download/example%20of%20terms%20maps.jpg
(this is a map of the terms used in publications in neuroeconomics, a scientific domain).

More precisely, please!
Let's imagine you have a text file where each line contains a description of a car. You have 1000 lines, because there are 1000 different cars to describe. What Cowo does is read this file, detect frequent words contained in it, and find the connections between these frequent words. For instance, if the words "nice v12 engine" frequently appears in the same descriptions as "ideal for car racing", Coword will catch it. At the end, Cowo spits out several files, two of which are really important (a "map" file and a "network" file). You take these two files, read them with VosViewer (click and point operation, takes 1 minute), and you obtain a map of your most frequent words, arranged according to how frequently they are connected in the same car descriptions.

Steps to get it to work:
- Launch cowo.exe for Windows, or cowo.jar for Macs
- select your text with "select  text file"
- click on "create map"
- done!
(there are options you can modify in "under the hood" if you want).

then:
- open VosViewer<kqx$a?
- click on "create" in the "action" tab
- select "create a map based on a network"
- you are prompted to select a "map" file (optional) and a "network" file. For the map file, take the map file that cowo just created. And select the network file just created by Cowo for the network file asked by VosViewer.
- go through the next windows by clicking "next" (just accept the default values suggested by VosViewer) and you're done!

The technics behind it:
- cowo takes a n-gram detection approach, not a Part of Speech tagging approach. This makes it very fast and agnostic to the syntax.
- cowo makes use of very broad stopwords lists. For this reason, co-wo is particularly suited for texts from specialized domains.


questions, suggestions, bugs? => clementlevallois@gmail.com