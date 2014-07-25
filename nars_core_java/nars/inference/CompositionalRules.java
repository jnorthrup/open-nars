/*
 * CompositionalRules.java
 *
 * Copyright (C) 2008  Pei Wang
 *
 * This file is part of Open-NARS.
 *
 * Open-NARS is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 2 of the License, or
 * (at your option) any later version.
 *
 * Open-NARS is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the abduction warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Open-NARS.  If not, see <http://www.gnu.org/licenses/>.
 */
package nars.inference;

import java.util.*;

import nars.entity.*;
import nars.entity.Task;
import nars.io.Symbols;
import nars.language.*;
import nars.storage.Memory;

/**
 * Compound term composition and decomposition rules, with two premises.
 * <p>
 * Forward inference only, except the last group (dependent variable
 * introduction) can also be used backward.
 */
public final class CompositionalRules {
    
    static Term unwrapNegation(Term T) //negation is not counting as depth
    {
        if(T!=null && T instanceof Negation)
            return (Term) ((CompoundTerm)T).getComponents().get(0).clone();
        return T;
    }
    public static Random rand = new Random(1);
    static boolean dedSecondLayerVariableUnification(Task task, Memory memory)
    {
        Sentence taskSentence=task.getSentence();
        Term taskterm=taskSentence.getContent();
        if(taskSentence==null || taskSentence.isQuestion()) {
            return false;
        }
        if(taskterm instanceof CompoundTerm && (taskterm instanceof Disjunction || taskterm instanceof Conjunction || taskterm instanceof Equivalence || taskterm instanceof Implication)) { //lets just allow conjunctions, implication and equivalence for now
            if(!Variable.containVar(taskterm.toString())) {
                return false;
            }            
            Concept second=memory.getConceptBag().takeOut();
            if(second==null) {
                return false;
            }
            Term secterm=second.getTerm();
            if(second.getBeliefs()==null || second.getBeliefs().size()==0) {
                return false;
            }
            
            Sentence second_belief=second.getBeliefs().get(rand.nextInt(second.getBeliefs().size()));
            TruthValue truthSecond=second_belief.getTruth();
            //we have to select a random belief
            ArrayList<HashMap<Term, Term>> terms_dependent=new ArrayList<HashMap<Term, Term>>();
            ArrayList<HashMap<Term, Term>> terms_independent=new ArrayList<HashMap<Term, Term>>();
            //ok, we have selected a second concept, we know the truth value of a belief of it, lets now go through taskterms components
            //for two levels, and remember the terms which unify with second
            ArrayList<Term> components_level1=((CompoundTerm) taskterm).getComponents();
            Term secterm_unwrap=unwrapNegation(secterm);
            for(Term T1 : components_level1) {
                Term T1_unwrap=unwrapNegation(T1);
                HashMap<Term, Term> Values = new HashMap<Term, Term>(); //we are only interested in first variables
                if(Variable.findSubstitute(Symbols.VAR_DEPENDENT, T1_unwrap, secterm_unwrap,Values,new HashMap<Term, Term>())) { 
                    terms_dependent.add(Values);
                }
                HashMap<Term, Term> Values2 = new HashMap<Term, Term>(); //we are only interested in first variables
                if(Variable.findSubstitute(Symbols.VAR_INDEPENDENT, T1_unwrap, secterm_unwrap,Values2,new HashMap<Term, Term>())) {
                    terms_independent.add(Values2);
                }
                if(!((T1_unwrap instanceof Implication) || (T1_unwrap instanceof Equivalence) || (T1_unwrap instanceof Conjunction) || (T1_unwrap instanceof Disjunction))) {
                    continue;
                }
                if(T1_unwrap instanceof CompoundTerm) {
                    ArrayList<Term> components_level2=((CompoundTerm) T1_unwrap).getComponents();
                    for(Term T2 : components_level2) {
                        Term T2_unwrap=unwrapNegation(T2);  
                        HashMap<Term, Term> Values3 = new HashMap<Term, Term>(); //we are only interested in first variables
                        if(Variable.findSubstitute(Symbols.VAR_DEPENDENT, T2_unwrap, secterm_unwrap,Values3,new HashMap<Term, Term>())) {
                            terms_dependent.add(Values3);
                        }
                        HashMap<Term, Term> Values4 = new HashMap<Term, Term>(); //we are only interested in first variables
                        if(Variable.findSubstitute(Symbols.VAR_INDEPENDENT, T2_unwrap, secterm_unwrap,Values4,new HashMap<Term, Term>())) {
                            terms_independent.add(Values4);
                        }
                    }
                }
            }
            Term result;
            TruthValue truth;
            if(!terms_dependent.isEmpty()) { //dependent or independent
                if(terms_dependent.isEmpty()) {
                    return false;
                }
                HashMap<Term, Term> substi=terms_dependent.get(rand.nextInt(terms_dependent.size()));
                result=(CompoundTerm)taskterm.clone();
                ((CompoundTerm)result).applySubstitute(substi);
                truth=TruthFunctions.anonymousAnalogy(taskSentence.getTruth(), truthSecond);
                
                Sentence newSentence = new Sentence(result, Symbols.JUDGMENT_MARK, truth, taskSentence.getStamp());
                newSentence.getStamp().creationTime=memory.getTime();
                Stamp useEvidentalbase=new Stamp(taskSentence.getStamp(),second_belief.getStamp(),memory.getTime());
                newSentence.getStamp().setEvidentalBase(useEvidentalbase.getEvidentalBase());
                newSentence.getStamp().setBaseLength(useEvidentalbase.getBaseLength());
                BudgetValue budget = BudgetFunctions.compoundForward(truth, newSentence.getContent(), memory);
                Task newTask = new Task(newSentence, budget, task, null);
                Task dummy = new Task(second_belief, budget, task, null);
                memory.currentBelief=taskSentence;
                memory.currentTask=dummy;
                memory.derivedTask(newTask, false, false);
            }
            if(!terms_independent.isEmpty()) {
                HashMap<Term, Term> substi=terms_independent.get(rand.nextInt(terms_independent.size()));
                result=(CompoundTerm)taskterm.clone();
                ((CompoundTerm)result).applySubstitute(substi);
                truth=TruthFunctions.deduction(taskSentence.getTruth(), truthSecond);
                
                Sentence newSentence = new Sentence(result, Symbols.JUDGMENT_MARK, truth, taskSentence.getStamp());
                newSentence.getStamp().creationTime=memory.getTime();
                Stamp useEvidentalbase=new Stamp(taskSentence.getStamp(),second_belief.getStamp(),memory.getTime());
                newSentence.getStamp().setEvidentalBase(useEvidentalbase.getEvidentalBase());
                newSentence.getStamp().setBaseLength(useEvidentalbase.getBaseLength());
                BudgetValue budget = BudgetFunctions.compoundForward(truth, newSentence.getContent(), memory);
                Task newTask = new Task(newSentence, budget, task, null);
                Task dummy = new Task(second_belief, budget, task, null);
                memory.currentBelief=taskSentence;
                memory.currentTask=dummy;
                memory.derivedTask(newTask, false, false);
            }
            return true;
        }
        return true;
    }
    
    /* -------------------- questions which contain answers which are of no value for NARS but need to be answered -------------------- */
    /**
     * {<(*,p1,p2,p3) <-> (*,s1,s2,s3)>?, <(*,p1,p2) <-> (*,s1,s2)>,<(*,p1,p3) <-> (*,s1,s3)>} |- {<(*,p1,p2,p3) <-> (*,s1,s2,s3)>}
     * 
     * @param sentence The first premise
     * @param belief The second premise
     * @param memory Reference to the memory
     */
    static boolean dedProductByQuestion(Task task, Memory memory) {
        Sentence sentence=task.getSentence();
        if(!sentence.isQuestion())
            return false;
        Term sent = sentence.getContent();
        if(!(sent instanceof Inheritance) && !(sent instanceof Similarity)) {
            return false;
        }
        boolean inherit=true;
        if(sent instanceof Similarity) {
            inherit=false;
        }
        Term subject=((Statement)sent).getSubject();
        Term predicate=((Statement)sent).getPredicate();
        if(!(subject instanceof Product) || !(predicate instanceof Product)) {
            return false;
        }
        Product S=(Product) subject;
        Product P=(Product) predicate;
        if(S.getComponents().size() != P.getComponents().size()) {
            return false;
        }
        ArrayList<Statement> needed=new ArrayList<Statement>();
        for(int i=0;i<S.getComponents().size();i++) {
            if(inherit) {
               needed.add(Inheritance.make(S.getComponents().get(i), P.getComponents().get(i), memory));
            }
            else {
               needed.add(Similarity.make(S.getComponents().get(i), P.getComponents().get(i), memory));
            }
        }
        ArrayList<LinkedList<Concept>> bag=memory.getConceptBag().getItemTable();
        for(LinkedList<Concept> baglevel : bag) {
            for(Concept concept : baglevel) {
                if(concept==null) {
                    continue;
                }
                for(Sentence belief : concept.getBeliefs()) {
                    if(belief==null) {
                        continue;
                    }
                    for(LinkedList<Concept> baglevel2 : bag) {
                        for(Concept concept2 : baglevel) {
                            if(concept2==null) {
                                continue;
                            }
                            for(Sentence belief2 : concept2.getBeliefs()) {
                                if(belief2==null) {
                                    continue;
                                }
                                
                                if((!(belief.getContent() instanceof Similarity) && !(belief.getContent() instanceof Inheritance)) || 
                                   (!(belief2.getContent() instanceof Similarity) && !(belief2.getContent() instanceof Inheritance))) {
                                    continue;
                                }
                                
                                Term subject1=((Statement)belief.getContent()).getSubject();
                                Term predicate1=((Statement)belief.getContent()).getPredicate();
                                Term subject2=((Statement)belief2.getContent()).getSubject();
                                Term predicate2=((Statement)belief2.getContent()).getPredicate();
                                
                                if(((subject1 instanceof Product) && !(predicate1 instanceof Product)) ||
                                   ((subject2 instanceof Product) && !(predicate2 instanceof Product)))
                                    continue;
                                
                                if((!(subject1 instanceof Product) && (predicate1 instanceof Product)) ||
                                   (!(subject2 instanceof Product) && (predicate2 instanceof Product)))
                                    continue;
                                
                                if(subject1 instanceof Product) {
                                    if(((CompoundTerm) predicate1).getComponents().size()!=
                                       ((CompoundTerm) subject1).getComponents().size()) {
                                        continue;
                                    } 
                                }
                                
                                if(subject2 instanceof Product) {
                                    if(((CompoundTerm) predicate2).getComponents().size()!=
                                       ((CompoundTerm) subject2).getComponents().size()) {
                                        continue;
                                    } 
                                }
                                
                                ArrayList<Integer> finished=new ArrayList<Integer>(); //already met
                                
                                if(inherit)
                                {
                                    if(subject1 instanceof Product) {
                                        Product S1=(Product) subject1;
                                        Product P1=(Product) predicate1;
                                        for(int i=0;i<S1.getComponents().size();i++) {
                                            Inheritance inhi=Inheritance.make(S1.componentAt(i), P1.componentAt(i), memory);
                                            if(needed.contains(inhi)) {
                                                int j=finished.indexOf(inhi);
                                                finished.add(j);
                                                continue;
                                            }
                                        }
                                    }
                                    else {
                                        Inheritance inhi=Inheritance.make(subject1, predicate1, memory);
                                        if(needed.contains(inhi)) {
                                            int j=finished.indexOf(inhi);
                                            finished.add(j);
                                            continue;
                                        }
                                    }
                                    //second:
                                    if(subject2 instanceof Product) {
                                        Product S2=(Product) subject2;
                                        Product P2=(Product) predicate2;
                                        for(int i=0;i<S2.getComponents().size();i++) {
                                            Inheritance inhi=Inheritance.make(S2.componentAt(i), P2.componentAt(i), memory);
                                            if(needed.contains(inhi)) {
                                                int j=finished.indexOf(inhi);
                                                finished.add((Integer)j);
                                                continue;
                                            }
                                        }
                                    }
                                    else {
                                        Inheritance inhi=Inheritance.make(subject2, predicate2, memory);
                                        if(needed.contains(inhi)) {
                                            int j=finished.indexOf(inhi);
                                            finished.add((Integer)j);
                                            continue;
                                        }
                                    }
                                }
                                else
                                {
                                   if(subject1 instanceof Product) {
                                        Product S1=(Product) subject1;
                                        Product P1=(Product) predicate1;
                                        for(int i=0;i<S1.getComponents().size();i++) {
                                            Similarity inhi=Similarity.make(S1.componentAt(i), P1.componentAt(i), memory);
                                            Similarity inhi2=Similarity.make(P1.componentAt(i), S1.componentAt(i), memory);
                                            if(needed.contains(inhi)) {
                                                int j=needed.indexOf(inhi);
                                                finished.add(j);
                                                continue;
                                            }
                                            if(needed.contains(inhi2)) {
                                                int j=needed.indexOf(inhi2);
                                                finished.add(j);
                                                continue;
                                            }
                                        }
                                    }
                                    else {
                                        Similarity inhi=Similarity.make(subject1, predicate1, memory);
                                        Similarity inhi2=Similarity.make(predicate1, subject1, memory);
                                        if(needed.contains(inhi)) {
                                            int j=needed.indexOf(inhi);
                                            finished.add(j);
                                            continue;
                                        }
                                        if(needed.contains(inhi2)) {
                                            int j=needed.indexOf(inhi2);
                                            finished.add(j);
                                            continue;
                                        }
                                    }
                                    //second:
                                    if(subject2 instanceof Product) {
                                        Product S2=(Product) subject2;
                                        Product P2=(Product) predicate2;
                                        for(int i=0;i<S2.getComponents().size();i++) {
                                            Similarity inhi=Similarity.make(S2.componentAt(i), P2.componentAt(i), memory);
                                            Similarity inhi2=Similarity.make(P2.componentAt(i), S2.componentAt(i), memory);
                                            if(needed.contains(inhi)) {
                                                int j=needed.indexOf(inhi);
                                                finished.add((Integer)j);
                                                continue;
                                            }
                                            if(needed.contains(inhi2)) {
                                                int j=needed.indexOf(inhi2);
                                                finished.add((Integer)j);
                                                continue;
                                            }
                                        }
                                    }
                                    else {
                                        Similarity inhi=Similarity.make(subject2, predicate2, memory);
                                        Similarity inhi2=Similarity.make(predicate2, subject2, memory);
                                        if(needed.contains(inhi2)) {
                                            int j=needed.indexOf(inhi2);
                                            finished.add((Integer)j);
                                            continue;
                                        }
                                    }
                                }
                                //ok lets find out if we missed a index
                                boolean fin=true;
                                for(int i=0;i<needed.size();i++) {
                                    boolean somej=false;
                                    for(Integer j : finished) {
                                        if(j.equals(i)) {
                                            somej=true;
                                        }
                                    }
                                    if(!somej) {
                                        fin=false;
                                        break;
                                    }
                                }
                                if(!fin) {
                                    continue;
                                }
                                //ok succeeded, derivation with conjunction was possible, lets add it
                                TruthValue truthAnd = TruthFunctions.intersection(belief.getTruth(), belief2.getTruth());
                                BudgetValue budget = BudgetFunctions.compoundForward(truthAnd, sentence.getContent(), memory);
                                //memory.doublePremiseTask(sentence.getContent(), truthAnd, budget, new Stamp(belief.getStamp(),belief2.getStamp(),memory.getTime()));
                                
                                Sentence newSentence = new Sentence(sentence.getContent(), Symbols.JUDGMENT_MARK, truthAnd, belief2.getStamp());
                                newSentence.getStamp().creationTime=memory.getTime();
                                Stamp useEvidentalbase=new Stamp(belief.getStamp(),belief2.getStamp(),memory.getTime());
                                newSentence.getStamp().setEvidentalBase(useEvidentalbase.getEvidentalBase());
                                newSentence.getStamp().setBaseLength(useEvidentalbase.getBaseLength());
                                Task newTask = new Task(newSentence, budget, task, null);
                                Task dummy = new Task(belief2, budget, task, null);
                                memory.currentBelief=belief;
                                memory.currentTask=dummy;
                                memory.derivedTask(newTask, false, false);
                                return true;
                            }
                        }
                    }
                }
            }
        }
        return false;
    }
    
    /**
     * {(&&,A,B,...)?, A,B} |- {(&&,A,B)}
     * {(&&,A,_components_1_)?, (&&,_part_of_components_1_),A} |- {(&&,A,_part_of_components_1_,B)}
     * and also the case where both are conjunctions, all components need to be subterm of the question-conjunction
     * in order for the subterms of both conjunctions to be collected together.
     * 
     * @param sentence The first premise
     * @param belief The second premise
     * @param memory Reference to the memory
     */
    static void dedConjunctionByQuestion(Sentence sentence, Sentence belief, Memory memory) {
        if(sentence==null || belief==null || sentence.isQuestion() || belief.isQuestion()) {
            return;
        }
        Term term1 = sentence.getContent();
        Term term2 = belief.getContent();
        ArrayList<LinkedList<Concept>> bag=memory.getConceptBag().getItemTable();
        for(LinkedList<Concept> baglevel : bag) {
            for(Concept concept : baglevel) {
                for(Task question : concept.getQuestions()) {
                    if(question==null) {
                        continue;
                    }
                    Sentence qu=question.getSentence();
                    if(qu==null) {
                        continue;
                    }
                    Term pcontent = qu.getContent();
                    if(pcontent==null || !(pcontent instanceof Conjunction) || ((CompoundTerm)pcontent).containVar()) {
                        continue;
                    }
                    if(!(term1 instanceof Conjunction) && !(term2 instanceof Conjunction)) {
                        if(!((CompoundTerm)pcontent).containComponent(term1) || !((CompoundTerm)pcontent).containComponent(term2)) {
                            continue;
                        }
                    }
                    if(term1 instanceof Conjunction) {
                        if(!(term2 instanceof Conjunction) && !((CompoundTerm)pcontent).containComponent(term2)) {
                            continue;
                        }
                        if(((CompoundTerm)term1).containVar()) {
                            continue;
                        }
                        boolean contin=false;
                        for(Term t : ((CompoundTerm)term1).getComponents()) {
                            if(!((CompoundTerm)pcontent).containComponent(t)) {
                                contin=true;
                                break;
                            }
                        }
                        if(contin) {
                            continue;
                        }
                    }
                    if(term2 instanceof Conjunction) {
                        if(!(term1 instanceof Conjunction) && !((CompoundTerm)pcontent).containComponent(term1)) {
                            continue;
                        }
                        if(((CompoundTerm)term2).containVar()) {
                            continue;
                        }
                        boolean contin=false;
                        for(Term t : ((CompoundTerm)term2).getComponents()) {
                            if(!((CompoundTerm)pcontent).containComponent(t)) {
                                contin=true;
                                break;
                            }
                        }
                        if(contin) {
                            continue;
                        }
                    }
                    Term conj = Conjunction.make(term1, term2, memory);
                    if(conj.toString().contains(Character.toString(Symbols.VAR_INDEPENDENT)) || 
                       conj.toString().contains(Character.toString(Symbols.VAR_DEPENDENT))) {
                        continue;
                    }
                    TruthValue truthT = memory.currentTask.getSentence().getTruth();
                    TruthValue truthB = memory.currentBelief.getTruth();
                    if(truthT==null || truthB==null) {
                        return;
                    }
                    TruthValue truthAnd = TruthFunctions.intersection(truthT, truthB);
                    BudgetValue budget = BudgetFunctions.compoundForward(truthAnd, conj, memory);
                    memory.doublePremiseTask(conj, truthAnd, budget);
                    return;
                }
            }
        }
    }
    
    /* -------------------- intersections and differences -------------------- */
    /**
     * {<S ==> M>, <P ==> M>} |- {<(S|P) ==> M>, <(S&P) ==> M>, <(S-P) ==> M>,
     * <(P-S) ==> M>}
     *
     * @param taskSentence The first premise
     * @param belief The second premise
     * @param index The location of the shared term
     * @param memory Reference to the memory
     */
    static void composeCompound(Statement taskContent, Statement beliefContent, int index, Memory memory) {
        if ((!memory.currentTask.getSentence().isJudgment()) || (taskContent.getClass() != beliefContent.getClass())) {
            return;
        }
        Term componentT = taskContent.componentAt(1 - index);
        Term componentB = beliefContent.componentAt(1 - index);
        Term componentCommon = taskContent.componentAt(index);
        if ((componentT instanceof CompoundTerm) && ((CompoundTerm) componentT).containAllComponents(componentB)) {
            decomposeCompound((CompoundTerm) componentT, componentB, componentCommon, index, true, memory);
            return;
        } else if ((componentB instanceof CompoundTerm) && ((CompoundTerm) componentB).containAllComponents(componentT)) {
            decomposeCompound((CompoundTerm) componentB, componentT, componentCommon, index, false, memory);
            return;
        }
        TruthValue truthT = memory.currentTask.getSentence().getTruth();
        TruthValue truthB = memory.currentBelief.getTruth();
        TruthValue truthOr = TruthFunctions.union(truthT, truthB);
        TruthValue truthAnd = TruthFunctions.intersection(truthT, truthB);
        TruthValue truthDif = null;
        Term termOr = null;
        Term termAnd = null;
        Term termDif = null;
        if (index == 0) {
            if (taskContent instanceof Inheritance) {
                termOr = IntersectionInt.make(componentT, componentB, memory);
                termAnd = IntersectionExt.make(componentT, componentB, memory);
                if (truthB.isNegative()) {
                    if (!truthT.isNegative()) {
                        termDif = DifferenceExt.make(componentT, componentB, memory);
                        truthDif = TruthFunctions.intersection(truthT, TruthFunctions.negation(truthB));
                    }
                } else if (truthT.isNegative()) {
                    termDif = DifferenceExt.make(componentB, componentT, memory);
                    truthDif = TruthFunctions.intersection(truthB, TruthFunctions.negation(truthT));
                }
            } else if (taskContent instanceof Implication) {
                termOr = Disjunction.make(componentT, componentB, memory);
                termAnd = Conjunction.make(componentT, componentB, memory);
            }
            processComposed(taskContent, (Term) componentCommon.clone(), termOr, truthOr, memory);
            processComposed(taskContent, (Term) componentCommon.clone(), termAnd, truthAnd, memory);
            processComposed(taskContent, (Term) componentCommon.clone(), termDif, truthDif, memory);
        } else {    // index == 1
            if (taskContent instanceof Inheritance) {
                termOr = IntersectionExt.make(componentT, componentB, memory);
                termAnd = IntersectionInt.make(componentT, componentB, memory);
                if (truthB.isNegative()) {
                    if (!truthT.isNegative()) {
                        termDif = DifferenceInt.make(componentT, componentB, memory);
                        truthDif = TruthFunctions.intersection(truthT, TruthFunctions.negation(truthB));
                    }
                } else if (truthT.isNegative()) {
                    termDif = DifferenceInt.make(componentB, componentT, memory);
                    truthDif = TruthFunctions.intersection(truthB, TruthFunctions.negation(truthT));
                }
            } else if (taskContent instanceof Implication) {
                termOr = Conjunction.make(componentT, componentB, memory);
                termAnd = Disjunction.make(componentT, componentB, memory);
            }
            processComposed(taskContent, termOr, (Term) componentCommon.clone(), truthOr, memory);
            processComposed(taskContent, termAnd, (Term) componentCommon.clone(), truthAnd, memory);
            processComposed(taskContent, termDif, (Term) componentCommon.clone(), truthDif, memory);
        }
        if (taskContent instanceof Inheritance) {
            introVarOuter(taskContent, beliefContent, index, memory);//            introVarImage(taskContent, beliefContent, index, memory);
        }
    }

    /**
     * Finish composing implication term
     *
     * @param premise1 Type of the contentInd
     * @param subject Subject of contentInd
     * @param predicate Predicate of contentInd
     * @param truth TruthValue of the contentInd
     * @param memory Reference to the memory
     */
    private static void processComposed(Statement statement, Term subject, Term predicate, TruthValue truth, Memory memory) {
        if ((subject == null) || (predicate == null)) {
            return;
        }
        Term content = Statement.make(statement, subject, predicate, memory);
        if ((content == null) || content.equals(statement) || content.equals(memory.currentBelief.getContent())) {
            return;
        }
        BudgetValue budget = BudgetFunctions.compoundForward(truth, content, memory);
        memory.doublePremiseTask(content, truth, budget);
    }

    /**
     * {<(S|P) ==> M>, <P ==> M>} |- <S ==> M>
     *
     * @param implication The implication term to be decomposed
     * @param componentCommon The part of the implication to be removed
     * @param term1 The other term in the contentInd
     * @param index The location of the shared term: 0 for subject, 1 for
     * predicate
     * @param compoundTask Whether the implication comes from the task
     * @param memory Reference to the memory
     */
    private static void decomposeCompound(CompoundTerm compound, Term component, Term term1, int index, boolean compoundTask, Memory memory) {
        if ((compound instanceof Statement) || (compound instanceof ImageExt) || (compound instanceof ImageInt)) {
            return;
        }
        Term term2 = CompoundTerm.reduceComponents(compound, component, memory);
        if (term2 == null) {
            return;
        }
        Task task = memory.currentTask;
        Sentence sentence = task.getSentence();
        Sentence belief = memory.currentBelief;
        Statement oldContent = (Statement) task.getContent();
        TruthValue v1,
                v2;
        if (compoundTask) {
            v1 = sentence.getTruth();
            v2 = belief.getTruth();
        } else {
            v1 = belief.getTruth();
            v2 = sentence.getTruth();
        }
        TruthValue truth = null;
        Term content;
        if (index == 0) {
            content = Statement.make(oldContent, term1, term2, memory);
            if (content == null) {
                return;
            }
            if (oldContent instanceof Inheritance) {
                if (compound instanceof IntersectionExt) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if (compound instanceof IntersectionInt) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                } else if ((compound instanceof SetInt) && (component instanceof SetInt)) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if ((compound instanceof SetExt) && (component instanceof SetExt)) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                } else if (compound instanceof DifferenceExt) {
                    if (compound.componentAt(0).equals(component)) {
                        truth = TruthFunctions.reduceDisjunction(v2, v1);
                    } else {
                        truth = TruthFunctions.reduceConjunctionNeg(v1, v2);
                    }
                }
            } else if (oldContent instanceof Implication) {
                if (compound instanceof Conjunction) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if (compound instanceof Disjunction) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                }
            }
        } else {
            content = Statement.make(oldContent, term2, term1, memory);
            if (content == null) {
                return;
            }
            if (oldContent instanceof Inheritance) {
                if (compound instanceof IntersectionInt) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if (compound instanceof IntersectionExt) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                } else if ((compound instanceof SetExt) && (component instanceof SetExt)) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if ((compound instanceof SetInt) && (component instanceof SetInt)) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                } else if (compound instanceof DifferenceInt) {
                    if (compound.componentAt(1).equals(component)) {
                        truth = TruthFunctions.reduceDisjunction(v2, v1);
                    } else {
                        truth = TruthFunctions.reduceConjunctionNeg(v1, v2);
                    }
                }
            } else if (oldContent instanceof Implication) {
                if (compound instanceof Disjunction) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                } else if (compound instanceof Conjunction) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                }
            }
        }
        if (truth != null) {
            BudgetValue budget = BudgetFunctions.compoundForward(truth, content, memory);
            memory.doublePremiseTask(content, truth, budget);
        }
    }

    /**
     * {(||, S, P), P} |- S {(&&, S, P), P} |- S
     *
     * @param implication The implication term to be decomposed
     * @param componentCommon The part of the implication to be removed
     * @param compoundTask Whether the implication comes from the task
     * @param memory Reference to the memory
     */
    static void decomposeStatement(CompoundTerm compound, Term component, boolean compoundTask, Memory memory) {
        Task task = memory.currentTask;
        Sentence sentence = task.getSentence();
        Sentence belief = memory.currentBelief;
        Term content = CompoundTerm.reduceComponents(compound, component, memory);
        if (content == null) {
            return;
        }
        TruthValue truth = null;
        BudgetValue budget;
        if (sentence.isQuestion()) {
            budget = BudgetFunctions.compoundBackward(content, memory);
            memory.doublePremiseTask(content, truth, budget);
            // special inference to answer conjunctive questions with query variables
            if (Variable.containVarQuery(sentence.getContent().getName())) {
                Concept contentConcept = memory.termToConcept(content);
                if (contentConcept == null) {
                    return;
                }
                Sentence contentBelief = contentConcept.getBelief(task);
                if (contentBelief == null) {
                    return;
                }
                Task contentTask = new Task(contentBelief, task.getBudget());
                memory.currentTask = contentTask;
                Term conj = Conjunction.make(component, content, memory);
                truth = TruthFunctions.intersection(contentBelief.getTruth(), belief.getTruth());
                budget = BudgetFunctions.compoundForward(truth, conj, memory);
                memory.doublePremiseTask(conj, truth, budget);
            }
        } else {
            TruthValue v1, v2;
            if (compoundTask) {
                v1 = sentence.getTruth();
                v2 = belief.getTruth();
            } else {
                v1 = belief.getTruth();
                v2 = sentence.getTruth();
            }
            if (compound instanceof Conjunction) {
                if (sentence instanceof Sentence) {
                    truth = TruthFunctions.reduceConjunction(v1, v2);
                }
            } else if (compound instanceof Disjunction) {
                if (sentence instanceof Sentence) {
                    truth = TruthFunctions.reduceDisjunction(v1, v2);
                }
            } else {
                return;
            }
            budget = BudgetFunctions.compoundForward(truth, content, memory);
            memory.doublePremiseTask(content, truth, budget);
        }
    }

    /* --------------- rules used for variable introduction --------------- */
    /**
     * Introduce a dependent variable in an outer-layer conjunction
     *
     * @param taskContent The first premise <M --> S>
     * @param beliefContent The second premise <M --> P>
     * @param index The location of the shared term: 0 for subject, 1 for
     * predicate
     * @param memory Reference to the memory
     */
    private static void introVarOuter(Statement taskContent, Statement beliefContent, int index, Memory memory) {
        TruthValue truthT = memory.currentTask.getSentence().getTruth();
        TruthValue truthB = memory.currentBelief.getTruth();
        Variable varInd = new Variable("$varInd1");
        Variable varInd2 = new Variable("$varInd2");
        Term term11, term12, term21, term22, commonTerm;
        HashMap<Term, Term> subs = new HashMap<>();
        if (index == 0) {
            term11 = varInd;
            term21 = varInd;
            term12 = taskContent.getPredicate();
            term22 = beliefContent.getPredicate();
            if ((term12 instanceof ImageExt) && (term22 instanceof ImageExt)) {
                commonTerm = ((ImageExt) term12).getTheOtherComponent();
                if ((commonTerm == null) || !((ImageExt) term22).containTerm(commonTerm)) {
                    commonTerm = ((ImageExt) term22).getTheOtherComponent();
                    if ((commonTerm == null) || !((ImageExt) term12).containTerm(commonTerm)) {
                        commonTerm = null;
                    }
                }
                if (commonTerm != null) {
                    subs.put(commonTerm, varInd2);
                    ((ImageExt) term12).applySubstitute(subs);
                    ((ImageExt) term22).applySubstitute(subs);
                }
            }
        } else {
            term11 = taskContent.getSubject();
            term21 = beliefContent.getSubject();
            term12 = varInd;
            term22 = varInd;
            if ((term11 instanceof ImageInt) && (term21 instanceof ImageInt)) {
                commonTerm = ((ImageInt) term11).getTheOtherComponent();
                if ((commonTerm == null) || !((ImageInt) term21).containTerm(commonTerm)) {
                    commonTerm = ((ImageInt) term21).getTheOtherComponent();
                    if ((commonTerm == null) || !((ImageInt) term11).containTerm(commonTerm)) {
                        commonTerm = null;
                    }
                }
                if (commonTerm != null) {
                    subs.put(commonTerm, varInd2);
                    ((ImageInt) term11).applySubstitute(subs);
                    ((ImageInt) term21).applySubstitute(subs);
                }
            }
        }
        Statement state1 = Inheritance.make(term11, term12, memory);
        Statement state2 = Inheritance.make(term21, term22, memory);
        Term content = Implication.make(state1, state2, memory);
        if (content == null) {
            return;
        }
        TruthValue truth = TruthFunctions.induction(truthT, truthB);
        BudgetValue budget = BudgetFunctions.compoundForward(truth, content, memory);
        memory.doublePremiseTask(content, truth, budget);
        content = Implication.make(state2, state1, memory);
        truth = TruthFunctions.induction(truthB, truthT);
        budget = BudgetFunctions.compoundForward(truth, content, memory);
        memory.doublePremiseTask(content, truth, budget);
        content = Equivalence.make(state1, state2, memory);
        truth = TruthFunctions.comparison(truthT, truthB);
        budget = BudgetFunctions.compoundForward(truth, content, memory);
        memory.doublePremiseTask(content, truth, budget);
        Variable varDep = new Variable("#varDep");
        if (index == 0) {
            state1 = Inheritance.make(varDep, taskContent.getPredicate(), memory);
            state2 = Inheritance.make(varDep, beliefContent.getPredicate(), memory);
        } else {
            state1 = Inheritance.make(taskContent.getSubject(), varDep, memory);
            state2 = Inheritance.make(beliefContent.getSubject(), varDep, memory);
        }
        content = Conjunction.make(state1, state2, memory);
        truth = TruthFunctions.intersection(truthT, truthB);
        budget = BudgetFunctions.compoundForward(truth, content, memory);
        memory.doublePremiseTask(content, truth, budget, false);
    }

    /**
     * {<M --> S>, <C ==> <M --> P>>} |- <(&&, <#x --> S>, C) ==> <#x --> P>>
     * {<M --> S>, (&&, C, <M --> P>)} |- (&&, C, <<#x --> S> ==> <#x --> P>>)
     *
     * @param taskContent The first premise directly used in internal induction,
     * <M --> S>
     * @param beliefContent The componentCommon to be used as a premise in
     * internal induction, <M --> P>
     * @param oldCompound The whole contentInd of the first premise, Implication
     * or Conjunction
     * @param memory Reference to the memory
     */
    static void introVarInner(Statement premise1, Statement premise2, CompoundTerm oldCompound, Memory memory) {
        Task task = memory.currentTask;
        Sentence taskSentence = task.getSentence();
        if (!taskSentence.isJudgment() || (premise1.getClass() != premise2.getClass()) || oldCompound.containComponent(premise1)) {
            return;
        }
        Term subject1 = premise1.getSubject();
        Term subject2 = premise2.getSubject();
        Term predicate1 = premise1.getPredicate();
        Term predicate2 = premise2.getPredicate();
        Term commonTerm1, commonTerm2;
        if (subject1.equals(subject2)) {
            commonTerm1 = subject1;
            commonTerm2 = secondCommonTerm(predicate1, predicate2, 0);
        } else if (predicate1.equals(predicate2)) {
            commonTerm1 = predicate1;
            commonTerm2 = secondCommonTerm(subject1, subject2, 0);
        } else {
            return;
        }
        Sentence belief = memory.currentBelief;
        HashMap<Term, Term> substitute = new HashMap<>();
        substitute.put(commonTerm1, new Variable("#varDep2"));
        CompoundTerm content = (CompoundTerm) Conjunction.make(premise1, oldCompound, memory);
        content.applySubstitute(substitute);
        TruthValue truth = TruthFunctions.intersection(taskSentence.getTruth(), belief.getTruth());
        BudgetValue budget = BudgetFunctions.forward(truth, memory);
        memory.doublePremiseTask(content, truth, budget, false);
        substitute.clear();
        substitute.put(commonTerm1, new Variable("$varInd1"));
        if (commonTerm2 != null) {
            substitute.put(commonTerm2, new Variable("$varInd2"));
        }
        content = Implication.make(premise1, oldCompound, memory);
        if (content == null) {
            return;
        }
        content.applySubstitute(substitute);
        if (premise1.equals(taskSentence.getContent())) {
            truth = TruthFunctions.induction(belief.getTruth(), taskSentence.getTruth());
        } else {
            truth = TruthFunctions.induction(taskSentence.getTruth(), belief.getTruth());
        }
        budget = BudgetFunctions.forward(truth, memory);
        memory.doublePremiseTask(content, truth, budget);
    }

    /**
     * Introduce a second independent variable into two terms with a common
     * component
     *
     * @param term1 The first term
     * @param term2 The second term
     * @param index The index of the terms in their statement
     */
    private static Term secondCommonTerm(Term term1, Term term2, int index) {
        Term commonTerm = null;
        if (index == 0) {
            if ((term1 instanceof ImageExt) && (term2 instanceof ImageExt)) {
                commonTerm = ((ImageExt) term1).getTheOtherComponent();
                if ((commonTerm == null) || !((ImageExt) term2).containTerm(commonTerm)) {
                    commonTerm = ((ImageExt) term2).getTheOtherComponent();
                    if ((commonTerm == null) || !((ImageExt) term1).containTerm(commonTerm)) {
                        commonTerm = null;
                    }
                }
            }
        } else {
            if ((term1 instanceof ImageInt) && (term2 instanceof ImageInt)) {
                commonTerm = ((ImageInt) term1).getTheOtherComponent();
                if ((commonTerm == null) || !((ImageInt) term2).containTerm(commonTerm)) {
                    commonTerm = ((ImageInt) term2).getTheOtherComponent();
                    if ((commonTerm == null) || !((ImageExt) term1).containTerm(commonTerm)) {
                        commonTerm = null;
                    }
                }
            }
        }
        return commonTerm;
    }
}
