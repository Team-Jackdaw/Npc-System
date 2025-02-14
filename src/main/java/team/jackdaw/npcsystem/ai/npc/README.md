# NPC

在这个包中应该实现一个NPC的大脑功能，包括Observation短期记忆，Memory长期记忆，Status当前状态，一个Reflect系统，一个Planner系统，一个ActionSelector系统。

并且该NPC应该具有交流能力并可以维护一个ConversationWindow，即根据他的Observation、Memory、Status的内容，能够与玩家或别的NPC进行交流。

## Status

Status用于储存该NPC的当前状态，这个状态应该是由一些数值和一些自然语言组成的，比如健康、财富、心情等（待定），也包含用自然语言描写的NPC的基础描述和性格描述。

## Observation

Observation用于储存该NPC的观察，这些观察应该是由自然语言组成的一段文字，且包含当前的时间，并且由Assistant对该观察打分并记录分数。

## Memory

Memory用于储存该NPC的记忆，该记忆由总分一定的一些Observation反思得到，应该也是由自然语言组成的一段文字且包含当前时间，相当于是过去一段时间该NPC观察到的事情的总结。

## Reflect

Reflect用于对一定总分的一些Observation进行反思，这个系统使用LLM模型对这些Observation中的内容进行分析，提取出其中的重要信息，并且对这些信息进行总结，这个总结应该是一段自然语言的文字。

## Planner

Planner是每天NPC对当天行动的计划，这个计划由LLM模型根据该NPC的Memory中的内容和Status提炼出来，应该对当天每一个时间段都有一个详细的计划。

## ActionSelector

ActionSelector是NPC的下一步动作选择系统，这个系统应该根据Planner中的计划、Observation中最近观察到的事情、Memory中的相关内容、Status当前状态，使用LLM模型得到NPC当前应该选择的Action及其参数。

## ConversationWindow

当NPC需要与其他NPC或玩家交流的时候，可以维护一个对话窗口，根据他的Observation、Memory、Status、Relation、Event和Opinion交流。交流结束后，更新Relation、Observation、Status等。

## RelationTable

RelationTable用于维护NPC与其他NPC之间的关系，这个关系应该是一个二维表，其中的每一个元素应该包含一个数值，代表了两个NPC之间的关系强度以及他们关系的文字描述。

## Event和Opinion

Event由世界维护，如果该事件被NPC知晓，那么NPC会维护一个对该事件的Opinion，这个Opinion应该是一个自然语言的文字，表达了NPC对该事件的看法。当一个新事件被NPC知晓时，NPC应该进行思考并更新属性。