# Assistant

Assistant在Agent系统中只作为一个辅助角色，他的主要功能是对各种事情打分，他可以查询RAG表和各个Agent的属性，但是没有权限修改他们。

Assistant不可以与Agent沟通。

Assistant不存在记忆，他是一个LLM模型，但每次调用都是独立的。