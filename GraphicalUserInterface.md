# Launcher #

This is the start window, it shows you several options:
  * Run NARS GUI - runs OpenNARS with main GUI interface
  * Run Web GUI - runs OpenNARS with experimental web interface
  * Run several application examples. See ApplicationPrograms wiki page.
![http://open-nars.googlecode.com/svn/trunk/wiki/Launcher.png](http://open-nars.googlecode.com/svn/trunk/wiki/Launcher.png)

# Main Windows #

The main GUI.
The left window shows from left to right, from top to bottom:
  * The start button which activates the system.
  * A step button to apply a single inference step.
  * A finish button, which sets the system to maximum speed and maximum silence, in which case only executions and answers to input questions will show up in the output log. (recommended for question answering)
  * A volume slider, which controls how much priority a task needs to have in order to be shown in the output log.
  * A decision threshold slider.
  * Task Memory Duration, a forgetting parameter for tasklinks.
  * Belief Memory Duration, the same for beliefs.
  * Concept Memory Duration, the same for the entire concept.

Plotted are several different system parameters, like the CPU time the system needed for the different inference steps, the emotion indicators like happiness and busyness, when certain inference rules were applied and so on.

![http://open-nars.googlecode.com/svn/trunk/wiki/MainGUI.png](http://open-nars.googlecode.com/svn/trunk/wiki/MainGUI.png)

The right upper window shows the output of the system.
Output in white is already forming a concept (because it came through the novel tasks filter), in which case one can click it. In the opening window the truth value of the belief of most confidence (same for desire) is shown. Also a frequency-confidence plot of all other beliefs of this concept is shown. For beliefs also a time-plot is shown when one of the beliefs is a event.
The right lower window allows inputting Narsese.
If the input is invalid, it will be assumed to be natural language or other data.

# Task Tree #
Shows the tasks and their parent tasks at one place.

![http://open-nars.googlecode.com/svn/trunk/wiki/TaskTree.png](http://open-nars.googlecode.com/svn/trunk/wiki/TaskTree.png)

# Concept Network #
Shows all the concepts of the system, where the circles are the concepts and the squares are tasks. Green are tasklinks and red are termlinks. The size of the elements encodes their priority.

![http://open-nars.googlecode.com/svn/trunk/wiki/ConceptNetwork.png](http://open-nars.googlecode.com/svn/trunk/wiki/ConceptNetwork.png)

# Sentence Table #
Shows a listing of the sentences which were output of the system.
Here you can sort according to different things like priority,
also a plot is possible by selecting some and clicking on graph,
which shows how they are semantically related.

![http://open-nars.googlecode.com/svn/trunk/wiki/SentenceTable.png](http://open-nars.googlecode.com/svn/trunk/wiki/SentenceTable.png)

# Plugin Menu #
Shows the loaded plugins of the system and its configuration options.

![http://open-nars.googlecode.com/svn/trunk/wiki/PluginMenu.png](http://open-nars.googlecode.com/svn/trunk/wiki/PluginMenu.png)

See [Plugins](Plugins.md) wiki page.

# Experience Library & Basic Usage #

Especially for beginners who want to get used to the system by trying the basic NAL examples, the most straightfoward way to do so is the following:

![http://open-nars.googlecode.com/svn/trunk/wiki/BasicUsage.png](http://open-nars.googlecode.com/svn/trunk/wiki/BasicUsage.png)

Also the current experience can be recorded by the button in the left upper corner of the I/O window, or with Memory -> Save Experience.
To input custom statements just use the input window (the bottom blank part in the I/O window), a possible example input:
```
<{tom} --> cat>.
```
And when we want to load another experience file, we may at first want to reset NARS's memory with Memory -> Reset, so that the examples won't influence each other.

# Commandline Interface #

Since OpenNARS-1.6.2, if nars.sh (in run\_specific folder) is started,
then NARS runs in an interactive commandline mode. (nars.core.NARRun)
Input can be typed in and will be accepted after pressing enter at any time. To don't let NARS spam the console, the volume can be adjusted by entering for example
```
"*volume=0" 
```
and reset using
```
"***"
```

# IRC Interface #

Since OpenNARS-1.6.2, OpenNARS can also be used by multiple users at the same time in an IRC channel. (using NARS\_IRC\_Bot.py)
The NARS\_NLP\_IRC\_Bot.py also supports natural language input,
for example
```
'this is a test.
```
will be converted to
```
<(*,this,is,a,test) --> sentence>. :|:
```
so that NARS can reason about the sentence.

Examples using the IRC bot:

Natural Language Example (this works by reasoning directly about the structure of the sentences, the meaning is not known to the system here):
```
<patham9_> 'bit enters the stage. 
<patham9_> 'bit is dead now. 
<patham9_> 'bot enters the stage. 
<nars> bot is dead now 
```

Complex NLP example distinguishing words from the concepts they represent (mapping sentences to meaning in not necessary unambiguous way):
```
//the word food stands for the concept FOOD
<patham9_> <(*,food,FOOD) --> REPRESENT>.
//the word beer stands for the concept BEER
<patham9_> <(*,beer,BEER) --> REPRESENT>.
//saying "I want food"
<patham9_> (^say,I,want,food). :|:
 //leads to NARS having food (for example this is body feedback)
<patham9_> <(\,SELF,_,HAS) --> FOOD>. :|:
 //adding the goal that it should have beer (giving more priority by adding multiple time, I was impatient :) )
<patham9_> <(\,SELF,_,HAS) --> BEER>!
<patham9_> <(\,SELF,_,HAS) --> BEER>!
<patham9_> <(\,SELF,_,HAS) --> BEER>!
//leads to execution of saying "I want beer", supported by past experience namely by the observed temporal coherence, 
//and the knowledge that "beer" stands for its internal concept "BEER".
<nars> EXE: ^say([I, want, beer, SELF])
```