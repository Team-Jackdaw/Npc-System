# Agent

在这个包中应该实现一个Agent的基本功能，包括Observation短期记忆，Memory长期记忆，一个Reflect系统，一个Planner系统，一个Executor系统。

并且该Agent应该具有交流能力，即根据他的Observation、Memory的内容，能够与玩家或别的Agent进行交流。

## Observation

Observation用于储存该Agent的观察，这些观察应该是由自然语言组成的一段文字，且包含当前的时间，并且由Assistant对该观察打分并记录分数。

## Memory

Memory用于储存该Agent的记忆，该记忆由总分一定的一些Observation反思得到，应该也是由自然语言组成的一段文字且包含当前时间，相当于是过去一段时间该Agent观察到的事情的总结。

## Reflect

Reflect用于对一定总分的一些Observation进行反思，这个系统使用LLM模型对这些Observation中的内容进行分析，提取出其中的重要信息，并且对这些信息进行总结，这个总结应该是一段自然语言的文字。

## Planner

Planner是每天Agent对当天行动的计划，这个计划由LLM模型根据该Agent的Memory中的内容提炼出来，应该对当天每一个时间段都有一个详细的计划。

## Executor

Executor是Agent的执行系统，这个系统应该根据Planner中的计划、Observation中最近观察到的事情、Memory中的相关内容，使用LLM模型得到Agent当前应该选择的Action及其参数，这个系统应该是一个有限状态机。