# API 应用程序编程接口

## Endpoints 端点

- Generate a completion 生成补全
- Generate a chat completion 生成聊天完成
- Create a Model 创建模型
- List Local Models 列出本地型号
- Show Model Information 显示型号信息
- Copy a Model 复制模型
- Delete a Model 删除模型
- Pull a Model 拉模型
- Push a Model 推送模型
- Generate Embeddings 生成嵌入
- List Running Models 列出运行模型

## Conventions 惯例

### Model names 型号名称

Model names follow a `model:tag` format, where `model` can have an optional namespace such as `example/model`. Some examples are `orca-mini:3b-q4_1` and `llama3:70b`. The tag is optional and, if not provided, will default to `latest`. The tag is used to identify a specific version.
模型名称遵循`model:tag`格式，其中`model`可以具有可选的命名空间，例如`example/model` 。一些示例是`orca-mini:3b-q4_1`和`llama3:70b` 。该标签是可选的，如果未提供，则默认为`latest` 。该标签用于标识特定版本。

### Durations 持续时间

All durations are returned in nanoseconds.
所有持续时间均以纳秒为单位返回。

### Streaming responses 流式响应

Certain endpoints stream responses as JSON objects. Streaming can be disabled by providing `{"stream": false}` for these endpoints.
某些端点将响应作为 JSON 对象进行流式传输。可以通过为这些端点提供`{"stream": false}`来禁用流式传输。

## Generate a completion 生成补全

```
POST /api/generate
```

Generate a response for a given prompt with a provided model. This is a streaming endpoint, so there will be a series of responses. The final response object will include statistics and additional data from the request.
使用提供的模型生成给定提示的响应。这是一个流端点，因此会有一系列响应。最终响应对象将包括来自请求的统计信息和附加数据。

### Parameters 参数

- `model`: (required) the [model name](https://github.com/ollama/ollama/blob/main/docs/api.md#model-names)
  `model` ：（必填）[型号名称](https://github.com/ollama/ollama/blob/main/docs/api.md#model-names)
- `prompt`: the prompt to generate a response for
  `prompt` ：生成响应的提示
- `suffix`: the text after the model response
  `suffix` : 模型响应后的文本
- `images`: (optional) a list of base64-encoded images (for multimodal models such as `llava`)
  `images` ：（可选）base64 编码图像的列表（对于多模式模型，例如`llava` ）

Advanced parameters (optional):
高级参数（可选）：

- `format`: the format to return a response in. Currently the only accepted value is `json`
  `format` ：返回响应的格式。目前唯一接受的值是`json`
- `options`: additional model parameters listed in the documentation for the [Modelfile](https://github.com/ollama/ollama/blob/main/docs/modelfile.md#valid-parameters-and-values) such as `temperature`
  `options` ：[模型](https://github.com/ollama/ollama/blob/main/docs/modelfile.md#valid-parameters-and-values)文件文档中列出的其他模型参数，例如`temperature`
- `system`: system message to (overrides what is defined in the `Modelfile`)
  `system` ：系统消息（覆盖`Modelfile`中定义的内容）
- `template`: the prompt template to use (overrides what is defined in the `Modelfile`)
  `template` ：要使用的提示模板（覆盖`Modelfile`中定义的内容）
- `context`: the context parameter returned from a previous request to `/generate`, this can be used to keep a short conversational memory
  `context` ：从先前的请求返回的上下文参数`/generate` ，这可用于保留简短的会话记忆
- `stream`: if `false` the response will be returned as a single response object, rather than a stream of objects
  `stream` ：如果为`false`响应将作为单个响应对象返回，而不是对象流
- `raw`: if `true` no formatting will be applied to the prompt. You may choose to use the `raw` parameter if you are specifying a full templated prompt in your request to the API
  `raw` ：如果`true`则不会对提示应用任何格式。如果您在 API 请求中指定完整模板化提示，则可以选择使用`raw`参数
- `keep_alive`: controls how long the model will stay loaded into memory following the request (default: `5m`)
  `keep_alive` ：控制模型在请求后加载到内存中的时间（默认值： `5m` ）

#### JSON mode JSON模式

Enable JSON mode by setting the `format` parameter to `json`. This will structure the response as a valid JSON object. See the JSON mode [example](https://github.com/ollama/ollama/blob/main/docs/api.md#request-json-mode) below.
通过将`format`参数设置为`json`来启用 JSON 模式。这会将响应构造为有效的 JSON 对象。请参阅下面的 JSON 模式示例。

Important 重要的

It's important to instruct the model to use JSON in the `prompt`. Otherwise, the model may generate large amounts whitespace.
在`prompt`中指示模型使用 JSON 非常重要。否则，模型可能会生成大量空白。

### Examples 示例

#### Generate request (Streaming) 生成请求（流式传输）

##### Request 要求

```
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.2",
  "prompt": "Why is the sky blue?"
}'
```

##### Response 回复

A stream of JSON objects is returned:
返回 JSON 对象流：

```
{
  "model": "llama3.2",
  "created_at": "2023-08-04T08:52:19.385406455-07:00",
  "response": "The",
  "done": false
}
```



The final response in the stream also includes additional data about the generation:
流中的最终响应还包括有关生成的附加数据：

- `total_duration`: time spent generating the response
  `total_duration` ：生成响应所花费的时间
- `load_duration`: time spent in nanoseconds loading the model
  `load_duration` ：加载模型所花费的时间（以纳秒为单位）
- `prompt_eval_count`: number of tokens in the prompt
  `prompt_eval_count` ：提示中的标记数量
- `prompt_eval_duration`: time spent in nanoseconds evaluating the prompt
  `prompt_eval_duration` ：评估提示所花费的时间（以纳秒为单位）
- `eval_count`: number of tokens in the response
  `eval_count` ：响应中的令牌数量
- `eval_duration`: time in nanoseconds spent generating the response
  `eval_duration` ：生成响应所花费的时间（以纳秒为单位）
- `context`: an encoding of the conversation used in this response, this can be sent in the next request to keep a conversational memory
  `context` ：此响应中使用的对话编码，可以在下一个请求中发送以保留对话记忆
- `response`: empty if the response was streamed, if not streamed, this will contain the full response
  `response` ：如果响应是流式传输的，则为空，如果没有流式传输，则这将包含完整的响应

To calculate how fast the response is generated in tokens per second (token/s), divide `eval_count` / `eval_duration` * `10^9`.
要计算每秒生成响应的速度（令牌/秒），请除以`eval_count` / `eval_duration` * `10^9` 。

```
{
  "model": "llama3.2",
  "created_at": "2023-08-04T19:22:45.499127Z",
  "response": "",
  "done": true,
  "context": [1, 2, 3],
  "total_duration": 10706818083,
  "load_duration": 6338219291,
  "prompt_eval_count": 26,
  "prompt_eval_duration": 130079000,
  "eval_count": 259,
  "eval_duration": 4232710000
}
```

#### Request (No streaming) 请求（无流式传输）

##### Request 要求

A response can be received in one reply when streaming is off.
当流式传输关闭时，可以在一次回复中收到响应。

```
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.2",
  "prompt": "Why is the sky blue?",
  "stream": false
}'
```

##### Response 回复

If `stream` is set to `false`, the response will be a single JSON object:
如果`stream`设置为`false` ，则响应将是单个 JSON 对象：

```
{
  "model": "llama3.2",
  "created_at": "2023-08-04T19:22:45.499127Z",
  "response": "The sky is blue because it is the color of the sky.",
  "done": true,
  "context": [1, 2, 3],
  "total_duration": 5043500667,
  "load_duration": 5025959,
  "prompt_eval_count": 26,
  "prompt_eval_duration": 325953000,
  "eval_count": 290,
  "eval_duration": 4709213000
}
```

#### Request (with suffix) 请求（带后缀）

##### Request 要求

```
curl http://localhost:11434/api/generate -d '{
  "model": "codellama:code",
  "prompt": "def compute_gcd(a, b):",
  "suffix": "    return result",
  "options": {
    "temperature": 0
  },
  "stream": false
}'
```

##### Response 回复

```
{
  "model": "codellama:code",
  "created_at": "2024-07-22T20:47:51.147561Z",
  "response": "\n  if a == 0:\n    return b\n  else:\n    return compute_gcd(b % a, a)\n\ndef compute_lcm(a, b):\n  result = (a * b) / compute_gcd(a, b)\n",
  "done": true,
  "done_reason": "stop",
  "context": [...],
  "total_duration": 1162761250,
  "load_duration": 6683708,
  "prompt_eval_count": 17,
  "prompt_eval_duration": 201222000,
  "eval_count": 63,
  "eval_duration": 953997000
}
```

#### Request (JSON mode) 请求（JSON方式）

Important 重要的

When `format` is set to `json`, the output will always be a well-formed JSON object. It's important to also instruct the model to respond in JSON.
当`format`设置为`json`时，输出将始终是格式正确的 JSON 对象。指示模型以 JSON 进行响应也很重要。

##### Request 要求

```
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.2",
  "prompt": "What color is the sky at different times of the day? Respond using JSON",
  "format": "json",
  "stream": false
}'
```

##### Response 回复

```
{
  "model": "llama3.2",
  "created_at": "2023-11-09T21:07:55.186497Z",
  "response": "{\n\"morning\": {\n\"color\": \"blue\"\n},\n\"noon\": {\n\"color\": \"blue-gray\"\n},\n\"afternoon\": {\n\"color\": \"warm gray\"\n},\n\"evening\": {\n\"color\": \"orange\"\n}\n}\n",
  "done": true,
  "context": [1, 2, 3],
  "total_duration": 4648158584,
  "load_duration": 4071084,
  "prompt_eval_count": 36,
  "prompt_eval_duration": 439038000,
  "eval_count": 180,
  "eval_duration": 4196918000
}
```

The value of `response` will be a string containing JSON similar to:
`response`的值将是一个包含 JSON 的字符串，类似于：

```
{
  "morning": {
    "color": "blue"
  },
  "noon": {
    "color": "blue-gray"
  },
  "afternoon": {
    "color": "warm gray"
  },
  "evening": {
    "color": "orange"
  }
}
```

#### Request (with images) 请求（带图片）

To submit images to multimodal models such as `llava` or `bakllava`, provide a list of base64-encoded `images`:
要将图像提交到多模式模型（例如`llava`或`bakllava` ，请提供 base64 编码`images`的列表：

#### Request 要求

```
curl http://localhost:11434/api/generate -d '{
  "model": "llava",
  "prompt":"What is in this picture?",
  "stream": false,
  "images": ["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"]
}'
```

#### Response 回复

```
{
  "model": "llava",
  "created_at": "2023-11-03T15:36:02.583064Z",
  "response": "A happy cartoon character, which is cute and cheerful.",
  "done": true,
  "context": [1, 2, 3],
  "total_duration": 2938432250,
  "load_duration": 2559292,
  "prompt_eval_count": 1,
  "prompt_eval_duration": 2195557000,
  "eval_count": 44,
  "eval_duration": 736432000
}
```

#### Request (Raw Mode) 请求（原始模式）

In some cases, you may wish to bypass the templating system and provide a full prompt. In this case, you can use the `raw` parameter to disable templating. Also note that raw mode will not return a context.
在某些情况下，您可能希望绕过模板系统并提供完整的提示。在这种情况下，您可以使用`raw`参数来禁用模板。另请注意，原始模式不会返回上下文。

##### Request 要求

```
curl http://localhost:11434/api/generate -d '{
  "model": "mistral",
  "prompt": "[INST] why is the sky blue? [/INST]",
  "raw": true,
  "stream": false
}'
```

#### Request (Reproducible outputs) 请求（可重现的输出）

For reproducible outputs, set `seed` to a number:
对于可重复的输出，将`seed`设置为一个数字：

##### Request 要求

```
curl http://localhost:11434/api/generate -d '{
  "model": "mistral",
  "prompt": "Why is the sky blue?",
  "options": {
    "seed": 123
  }
}'
```

##### Response 回复

```
{
  "model": "mistral",
  "created_at": "2023-11-03T15:36:02.583064Z",
  "response": " The sky appears blue because of a phenomenon called Rayleigh scattering.",
  "done": true,
  "total_duration": 8493852375,
  "load_duration": 6589624375,
  "prompt_eval_count": 14,
  "prompt_eval_duration": 119039000,
  "eval_count": 110,
  "eval_duration": 1779061000
}
```

#### Generate request (With options) 生成请求（带选项）

If you want to set custom options for the model at runtime rather than in the Modelfile, you can do so with the `options` parameter. This example sets every available option, but you can set any of them individually and omit the ones you do not want to override.
如果您想在运行时而不是在模型文件中为模型设置自定义选项，则可以使用`options`参数来执行此操作。此示例设置每个可用选项，但您可以单独设置其中任何选项并忽略您不想覆盖的选项。

##### Request 要求

```
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.2",
  "prompt": "Why is the sky blue?",
  "stream": false,
  "options": {
    "num_keep": 5,
    "seed": 42,
    "num_predict": 100,
    "top_k": 20,
    "top_p": 0.9,
    "min_p": 0.0,
    "tfs_z": 0.5,
    "typical_p": 0.7,
    "repeat_last_n": 33,
    "temperature": 0.8,
    "repeat_penalty": 1.2,
    "presence_penalty": 1.5,
    "frequency_penalty": 1.0,
    "mirostat": 1,
    "mirostat_tau": 0.8,
    "mirostat_eta": 0.6,
    "penalize_newline": true,
    "stop": ["\n", "user:"],
    "numa": false,
    "num_ctx": 1024,
    "num_batch": 2,
    "num_gpu": 1,
    "main_gpu": 0,
    "low_vram": false,
    "vocab_only": false,
    "use_mmap": true,
    "use_mlock": false,
    "num_thread": 8
  }
}'
```

##### Response 回复

```
{
  "model": "llama3.2",
  "created_at": "2023-08-04T19:22:45.499127Z",
  "response": "The sky is blue because it is the color of the sky.",
  "done": true,
  "context": [1, 2, 3],
  "total_duration": 4935886791,
  "load_duration": 534986708,
  "prompt_eval_count": 26,
  "prompt_eval_duration": 107345000,
  "eval_count": 237,
  "eval_duration": 4289432000
}
```

#### Load a model 加载模型

If an empty prompt is provided, the model will be loaded into memory.
如果提供空提示，模型将被加载到内存中。

##### Request 要求

```
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.2"
}'
```

##### Response 回复

A single JSON object is returned:
返回单个 JSON 对象：

```
{
  "model": "llama3.2",
  "created_at": "2023-12-18T19:52:07.071755Z",
  "response": "",
  "done": true
}
```

#### Unload a model 卸载模型

If an empty prompt is provided and the `keep_alive` parameter is set to `0`, a model will be unloaded from memory.
如果提供空提示并将`keep_alive`参数设置为`0` ，则将从内存中卸载模型。

##### Request 要求

```
curl http://localhost:11434/api/generate -d '{
  "model": "llama3.2",
  "keep_alive": 0
}'
```

##### Response 回复

A single JSON object is returned:
返回单个 JSON 对象：

```
{
  "model": "llama3.2",
  "created_at": "2024-09-12T03:54:03.516566Z",
  "response": "",
  "done": true,
  "done_reason": "unload"
}
```

## Generate a chat completion 生成聊天完成

```
POST /api/chat
```

Generate the next message in a chat with a provided model. This is a streaming endpoint, so there will be a series of responses. Streaming can be disabled using `"stream": false`. The final response object will include statistics and additional data from the request.
在与提供的模型的聊天中生成下一条消息。这是一个流端点，因此会有一系列响应。可以使用`"stream": false`禁用流式传输。最终响应对象将包括来自请求的统计信息和附加数据。

### Parameters 参数

- `model`: (required) the [model name](https://github.com/ollama/ollama/blob/main/docs/api.md#model-names)
  `model` ：（必填）[型号名称](https://github.com/ollama/ollama/blob/main/docs/api.md#model-names)
- `messages`: the messages of the chat, this can be used to keep a chat memory
  `messages` : 聊天的消息，这可以用来保留聊天记忆
- `tools`: tools for the model to use if supported. Requires `stream` to be set to `false`
  `tools` ：模型使用的工具（如果支持）。需要将`stream`设置为`false`

The `message` object has the following fields:
`message`对象具有以下字段：

- `role`: the role of the message, either `system`, `user`, `assistant`, or `tool`
  `role` ：消息的角色，可以是`system` 、 `user` 、 `assistant`或`tool`
- `content`: the content of the message
  `content` ：消息的内容
- `images` (optional): a list of images to include in the message (for multimodal models such as `llava`)
  `images` （可选）：要包含在消息中的图像列表（对于多模式模型，例如`llava` ）
- `tool_calls` (optional): a list of tools the model wants to use
  `tool_calls` （可选）：模型想要使用的工具列表

Advanced parameters (optional):
高级参数（可选）：

- `format`: the format to return a response in. Currently the only accepted value is `json`
  `format` ：返回响应的格式。目前唯一接受的值是`json`
- `options`: additional model parameters listed in the documentation for the [Modelfile](https://github.com/ollama/ollama/blob/main/docs/modelfile.md#valid-parameters-and-values) such as `temperature`
  `options` ：[模型](https://github.com/ollama/ollama/blob/main/docs/modelfile.md#valid-parameters-and-values)文件文档中列出的其他模型参数，例如`temperature`
- `stream`: if `false` the response will be returned as a single response object, rather than a stream of objects
  `stream` ：如果为`false`响应将作为单个响应对象返回，而不是对象流
- `keep_alive`: controls how long the model will stay loaded into memory following the request (default: `5m`)
  `keep_alive` ：控制模型在请求后加载到内存中的时间（默认值： `5m` ）

### Examples 示例

#### Chat Request (Streaming) 聊天请求（流式传输）

##### Request 要求

Send a chat message with a streaming response.
发送带有流式响应的聊天消息。

```
curl http://localhost:11434/api/chat -d '{
  "model": "llama3.2",
  "messages": [
    {
      "role": "user",
      "content": "why is the sky blue?"
    }
  ]
}'
```

##### Response 回复

A stream of JSON objects is returned:
返回 JSON 对象流：

```
{
  "model": "llama3.2",
  "created_at": "2023-08-04T08:52:19.385406455-07:00",
  "message": {
    "role": "assistant",
    "content": "The",
    "images": null
  },
  "done": false
}
```

Final response: 最终回应：

```
{
  "model": "llama3.2",
  "created_at": "2023-08-04T19:22:45.499127Z",
  "done": true,
  "total_duration": 4883583458,
  "load_duration": 1334875,
  "prompt_eval_count": 26,
  "prompt_eval_duration": 342546000,
  "eval_count": 282,
  "eval_duration": 4535599000
}
```

#### Chat request (No streaming) 聊天请求（无流媒体）

##### Request 要求

```
curl http://localhost:11434/api/chat -d '{
  "model": "llama3.2",
  "messages": [
    {
      "role": "user",
      "content": "why is the sky blue?"
    }
  ],
  "stream": false
}'
```

##### Response 回复

```
{
  "model": "llama3.2",
  "created_at": "2023-12-12T14:13:43.416799Z",
  "message": {
    "role": "assistant",
    "content": "Hello! How are you today?"
  },
  "done": true,
  "total_duration": 5191566416,
  "load_duration": 2154458,
  "prompt_eval_count": 26,
  "prompt_eval_duration": 383809000,
  "eval_count": 298,
  "eval_duration": 4799921000
}
```

#### Chat request (With History) 聊天请求（有历史记录）

Send a chat message with a conversation history. You can use this same approach to start the conversation using multi-shot or chain-of-thought prompting.
发送带有对话历史记录的聊天消息。您可以使用相同的方法通过多重提示或思维链提示来开始对话。

##### Request 要求

```
curl http://localhost:11434/api/chat -d '{
  "model": "llama3.2",
  "messages": [
    {
      "role": "user",
      "content": "why is the sky blue?"
    },
    {
      "role": "assistant",
      "content": "due to rayleigh scattering."
    },
    {
      "role": "user",
      "content": "how is that different than mie scattering?"
    }
  ]
}'
```

##### Response 回复

A stream of JSON objects is returned:
返回 JSON 对象流：

```
{
  "model": "llama3.2",
  "created_at": "2023-08-04T08:52:19.385406455-07:00",
  "message": {
    "role": "assistant",
    "content": "The"
  },
  "done": false
}
```

Final response: 最终回应：

```
{
  "model": "llama3.2",
  "created_at": "2023-08-04T19:22:45.499127Z",
  "done": true,
  "total_duration": 8113331500,
  "load_duration": 6396458,
  "prompt_eval_count": 61,
  "prompt_eval_duration": 398801000,
  "eval_count": 468,
  "eval_duration": 7701267000
}
```

#### Chat request (with images) 聊天请求（带图像）

##### Request 要求

Send a chat message with images. The images should be provided as an array, with the individual images encoded in Base64.
发送带有图像的聊天消息。图像应作为数组提供，各个图像以 Base64 编码。

```
curl http://localhost:11434/api/chat -d '{
  "model": "llava",
  "messages": [
    {
      "role": "user",
      "content": "what is in this image?",
      "images": ["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"]
    }
  ]
}'
```

##### Response 回复

```
{
  "model": "llava",
  "created_at": "2023-12-13T22:42:50.203334Z",
  "message": {
    "role": "assistant",
    "content": " The image features a cute, little pig with an angry facial expression. It's wearing a heart on its shirt and is waving in the air. This scene appears to be part of a drawing or sketching project.",
    "images": null
  },
  "done": true,
  "total_duration": 1668506709,
  "load_duration": 1986209,
  "prompt_eval_count": 26,
  "prompt_eval_duration": 359682000,
  "eval_count": 83,
  "eval_duration": 1303285000
}
```

#### Chat request (Reproducible outputs) 聊天请求（可重现的输出）

##### Request 要求

```
curl http://localhost:11434/api/chat -d '{
  "model": "llama3.2",
  "messages": [
    {
      "role": "user",
      "content": "Hello!"
    }
  ],
  "options": {
    "seed": 101,
    "temperature": 0
  }
}'
```

##### Response 回复

```
{
  "model": "llama3.2",
  "created_at": "2023-12-12T14:13:43.416799Z",
  "message": {
    "role": "assistant",
    "content": "Hello! How are you today?"
  },
  "done": true,
  "total_duration": 5191566416,
  "load_duration": 2154458,
  "prompt_eval_count": 26,
  "prompt_eval_duration": 383809000,
  "eval_count": 298,
  "eval_duration": 4799921000
}
```

#### Chat request (with tools) 聊天请求（使用工具）

##### Request 要求

```
curl http://localhost:11434/api/chat -d '{
  "model": "llama3.2",
  "messages": [
    {
      "role": "user",
      "content": "What is the weather today in Paris?"
    }
  ],
  "stream": false,
  "tools": [
    {
      "type": "function",
      "function": {
        "name": "get_current_weather",
        "description": "Get the current weather for a location",
        "parameters": {
          "type": "object",
          "properties": {
            "location": {
              "type": "string",
              "description": "The location to get the weather for, e.g. San Francisco, CA"
            },
            "format": {
              "type": "string",
              "description": "The format to return the weather in, e.g. 'celsius' or 'fahrenheit'",
              "enum": ["celsius", "fahrenheit"]
            }
          },
          "required": ["location", "format"]
        }
      }
    }
  ]
}'
```

##### Response 回复

```
{
  "model": "llama3.2",
  "created_at": "2024-07-22T20:33:28.123648Z",
  "message": {
    "role": "assistant",
    "content": "",
    "tool_calls": [
      {
        "function": {
          "name": "get_current_weather",
          "arguments": {
            "format": "celsius",
            "location": "Paris, FR"
          }
        }
      }
    ]
  },
  "done_reason": "stop",
  "done": true,
  "total_duration": 885095291,
  "load_duration": 3753500,
  "prompt_eval_count": 122,
  "prompt_eval_duration": 328493000,
  "eval_count": 33,
  "eval_duration": 552222000
}
```

#### Load a model 加载模型

If the messages array is empty, the model will be loaded into memory.
如果消息数组为空，模型将被加载到内存中。

##### Request 要求

```
curl http://localhost:11434/api/chat -d '{
  "model": "llama3.2",
  "messages": []
}'
```

##### Response 回复

```
{
  "model": "llama3.2",
  "created_at":"2024-09-12T21:17:29.110811Z",
  "message": {
    "role": "assistant",
    "content": ""
  },
  "done_reason": "load",
  "done": true
}
```

#### Unload a model 卸载模型

If the messages array is empty and the `keep_alive` parameter is set to `0`, a model will be unloaded from memory.
如果 messages 数组为空并且`keep_alive`参数设置为`0` ，则模型将从内存中卸载。

##### Request 要求

```
curl http://localhost:11434/api/chat -d '{
  "model": "llama3.2",
  "messages": [],
  "keep_alive": 0
}'
```

##### Response 回复

A single JSON object is returned:
返回单个 JSON 对象：

```
{
  "model": "llama3.2",
  "created_at":"2024-09-12T21:33:17.547535Z",
  "message": {
    "role": "assistant",
    "content": ""
  },
  "done_reason": "unload",
  "done": true
}
```

## Create a Model 创建模型

```
POST /api/create
```

Create a model from a [`Modelfile`](https://github.com/ollama/ollama/blob/main/docs/modelfile.md). It is recommended to set `modelfile` to the content of the Modelfile rather than just set `path`. This is a requirement for remote create. Remote model creation must also create any file blobs, fields such as `FROM` and `ADAPTER`, explicitly with the server using [Create a Blob](https://github.com/ollama/ollama/blob/main/docs/api.md#create-a-blob) and the value to the path indicated in the response.
从[`Modelfile`](https://github.com/ollama/ollama/blob/main/docs/modelfile.md)创建模型。建议将`modelfile`设置为 Modelfile 的内容，而不仅仅是设置`path` 。这是远程创建的要求。远程模型创建还必须使用[“创建 Blob”](https://github.com/ollama/ollama/blob/main/docs/api.md#create-a-blob)和响应中指示的路径的值显式使用服务器创建任何文件 blob、字段（例如`FROM`和`ADAPTER` 。

### Parameters 参数

- `model`: name of the model to create
  `model` ：要创建的模型的名称
- `modelfile` (optional): contents of the Modelfile
  `modelfile` （可选）：模型文件的内容
- `stream`: (optional) if `false` the response will be returned as a single response object, rather than a stream of objects
  `stream` ：（可选）如果为`false`响应将作为单个响应对象返回，而不是对象流
- `path` (optional): path to the Modelfile
  `path` （可选）：模型文件的路径
- `quantize` (optional): quantize a non-quantized (e.g. float16) model
  `quantize` （可选）：量化非量化（例如 float16）模型

#### Quantization types 量化类型

Type 类型Recommended 受到推崇的q2_Kq3_K_Lq3_K_Mq3_K_Sq4_0q4_1q4_K_M*q4_K_Sq5_0q5_1q5_K_Mq5_K_Sq6_Kq8_0*

### Examples 示例

#### Create a new model 创建一个新模型

Create a new model from a `Modelfile`.
从`Modelfile`创建一个新模型。

##### Request 要求

```
curl http://localhost:11434/api/create -d '{
  "model": "mario",
  "modelfile": "FROM llama3\nSYSTEM You are mario from Super Mario Bros."
}'
```

##### Response 回复

A stream of JSON objects is returned:
返回 JSON 对象流：

```
{"status":"reading model metadata"}
{"status":"creating system layer"}
{"status":"using already created layer sha256:22f7f8ef5f4c791c1b03d7eb414399294764d7cc82c7e94aa81a1feb80a983a2"}
{"status":"using already created layer sha256:8c17c2ebb0ea011be9981cc3922db8ca8fa61e828c5d3f44cb6ae342bf80460b"}
{"status":"using already created layer sha256:7c23fb36d80141c4ab8cdbb61ee4790102ebd2bf7aeff414453177d4f2110e5d"}
{"status":"using already created layer sha256:2e0493f67d0c8c9c68a8aeacdf6a38a2151cb3c4c1d42accf296e19810527988"}
{"status":"using already created layer sha256:2759286baa875dc22de5394b4a925701b1896a7e3f8e53275c36f75a877a82c9"}
{"status":"writing layer sha256:df30045fe90f0d750db82a058109cecd6d4de9c90a3d75b19c09e5f64580bb42"}
{"status":"writing layer sha256:f18a68eb09bf925bb1b669490407c1b1251c5db98dc4d3d81f3088498ea55690"}
{"status":"writing manifest"}
{"status":"success"}
```

#### Quantize a model 量化模型

Quantize a non-quantized model.
量化非量化模型。

##### Request 要求

```
curl http://localhost:11434/api/create -d '{
  "model": "llama3.1:quantized",
  "modelfile": "FROM llama3.1:8b-instruct-fp16",
  "quantize": "q4_K_M"
}'
```

##### Response 回复

A stream of JSON objects is returned:
返回 JSON 对象流：

```
{"status":"quantizing F16 model to Q4_K_M"}
{"status":"creating new layer sha256:667b0c1932bc6ffc593ed1d03f895bf2dc8dc6df21db3042284a6f4416b06a29"}
{"status":"using existing layer sha256:11ce4ee3e170f6adebac9a991c22e22ab3f8530e154ee669954c4bc73061c258"}
{"status":"using existing layer sha256:0ba8f0e314b4264dfd19df045cde9d4c394a52474bf92ed6a3de22a4ca31a177"}
{"status":"using existing layer sha256:56bb8bd477a519ffa694fc449c2413c6f0e1d3b1c88fa7e3c9d88d3ae49d4dcb"}
{"status":"creating new layer sha256:455f34728c9b5dd3376378bfb809ee166c145b0b4c1f1a6feca069055066ef9a"}
{"status":"writing manifest"}
{"status":"success"}
```

### Check if a Blob Exists 检查 Blob 是否存在

```
HEAD /api/blobs/:digest
```

Ensures that the file blob used for a FROM or ADAPTER field exists on the server. This is checking your Ollama server and not ollama.com.
确保服务器上存在用于 FROM 或 ADAPTER 字段的文件 blob。这是检查您的 Ollama 服务器而不是 ollama.com。

#### Query Parameters 查询参数

- `digest`: the SHA256 digest of the blob
  `digest` ：blob 的 SHA256 摘要

#### Examples 示例

##### Request 要求

```
curl -I http://localhost:11434/api/blobs/sha256:29fdb92e57cf0827ded04ae6461b5931d01fa595843f55d36f5b275a52087dd2
```

##### Response 回复

Return 200 OK if the blob exists, 404 Not Found if it does not.
如果 blob 存在，则返回 200 OK；如果不存在，则返回 404 Not Found。

### Create a Blob 创建一个斑点

```
POST /api/blobs/:digest
```

Create a blob from a file on the server. Returns the server file path.
从服务器上的文件创建 blob。返回服务器文件路径。

#### Query Parameters 查询参数

- `digest`: the expected SHA256 digest of the file
  `digest` ：文件的预期 SHA256 摘要

#### Examples 示例

##### Request 要求

```
curl -T model.bin -X POST http://localhost:11434/api/blobs/sha256:29fdb92e57cf0827ded04ae6461b5931d01fa595843f55d36f5b275a52087dd2
```

##### Response 回复

Return 201 Created if the blob was successfully created, 400 Bad Request if the digest used is not expected.
如果已成功创建 blob，则返回 201 Created；如果使用的摘要不是预期的，则返回 400 Bad Request。

## List Local Models 列出本地型号

```
GET /api/tags
```

List models that are available locally.
列出本地可用的型号。

### Examples 示例

#### Request 要求

```
curl http://localhost:11434/api/tags
```

#### Response 回复

A single JSON object will be returned.
将返回单个 JSON 对象。

```
{
  "models": [
    {
      "name": "codellama:13b",
      "modified_at": "2023-11-04T14:56:49.277302595-07:00",
      "size": 7365960935,
      "digest": "9f438cb9cd581fc025612d27f7c1a6669ff83a8bb0ed86c94fcf4c5440555697",
      "details": {
        "format": "gguf",
        "family": "llama",
        "families": null,
        "parameter_size": "13B",
        "quantization_level": "Q4_0"
      }
    },
    {
      "name": "llama3:latest",
      "modified_at": "2023-12-07T09:32:18.757212583-08:00",
      "size": 3825819519,
      "digest": "fe938a131f40e6f6d40083c9f0f430a515233eb2edaa6d72eb85c50d64f2300e",
      "details": {
        "format": "gguf",
        "family": "llama",
        "families": null,
        "parameter_size": "7B",
        "quantization_level": "Q4_0"
      }
    }
  ]
}
```

## Show Model Information 显示型号信息

```
POST /api/show
```

Show information about a model including details, modelfile, template, parameters, license, system prompt.
显示有关模型的信息，包括详细信息、模型文件、模板、参数、许可证、系统提示。

### Parameters 参数

- `model`: name of the model to show
  `model` ：要显示的模型名称
- `verbose`: (optional) if set to `true`, returns full data for verbose response fields
  `verbose` ：（可选）如果设置为`true` ，则返回详细响应字段的完整数据

### Examples 示例

#### Request 要求

```
curl http://localhost:11434/api/show -d '{
  "model": "llama3.2"
}'
```

#### Response 回复

```
{
  "modelfile": "# Modelfile generated by \"ollama show\"\n# To build a new Modelfile based on this one, replace the FROM line with:\n# FROM llava:latest\n\nFROM /Users/matt/.ollama/models/blobs/sha256:200765e1283640ffbd013184bf496e261032fa75b99498a9613be4e94d63ad52\nTEMPLATE \"\"\"{{ .System }}\nUSER: {{ .Prompt }}\nASSISTANT: \"\"\"\nPARAMETER num_ctx 4096\nPARAMETER stop \"\u003c/s\u003e\"\nPARAMETER stop \"USER:\"\nPARAMETER stop \"ASSISTANT:\"",
  "parameters": "num_keep                       24\nstop                           \"<|start_header_id|>\"\nstop                           \"<|end_header_id|>\"\nstop                           \"<|eot_id|>\"",
  "template": "{{ if .System }}<|start_header_id|>system<|end_header_id|>\n\n{{ .System }}<|eot_id|>{{ end }}{{ if .Prompt }}<|start_header_id|>user<|end_header_id|>\n\n{{ .Prompt }}<|eot_id|>{{ end }}<|start_header_id|>assistant<|end_header_id|>\n\n{{ .Response }}<|eot_id|>",
  "details": {
    "parent_model": "",
    "format": "gguf",
    "family": "llama",
    "families": [
      "llama"
    ],
    "parameter_size": "8.0B",
    "quantization_level": "Q4_0"
  },
  "model_info": {
    "general.architecture": "llama",
    "general.file_type": 2,
    "general.parameter_count": 8030261248,
    "general.quantization_version": 2,
    "llama.attention.head_count": 32,
    "llama.attention.head_count_kv": 8,
    "llama.attention.layer_norm_rms_epsilon": 0.00001,
    "llama.block_count": 32,
    "llama.context_length": 8192,
    "llama.embedding_length": 4096,
    "llama.feed_forward_length": 14336,
    "llama.rope.dimension_count": 128,
    "llama.rope.freq_base": 500000,
    "llama.vocab_size": 128256,
    "tokenizer.ggml.bos_token_id": 128000,
    "tokenizer.ggml.eos_token_id": 128009,
    "tokenizer.ggml.merges": [],            // populates if `verbose=true`
    "tokenizer.ggml.model": "gpt2",
    "tokenizer.ggml.pre": "llama-bpe",
    "tokenizer.ggml.token_type": [],        // populates if `verbose=true`
    "tokenizer.ggml.tokens": []             // populates if `verbose=true`
  }
}
```

## Copy a Model 复制模型

```
POST /api/copy
```

Copy a model. Creates a model with another name from an existing model.
复制模型。使用现有模型的另一个名称创建模型。

### Examples 示例

#### Request 要求

```
curl http://localhost:11434/api/copy -d '{
  "source": "llama3.2",
  "destination": "llama3-backup"
}'
```

#### Response 回复

Returns a 200 OK if successful, or a 404 Not Found if the source model doesn't exist.
如果成功，则返回 200 OK；如果源模型不存在，则返回 404 Not Found。

## Delete a Model 删除模型

```
DELETE /api/delete
```

Delete a model and its data.
删除模型及其数据。

### Parameters 参数

- `model`: model name to delete
  `model` : 要删除的型号名称

### Examples 示例

#### Request 要求

```
curl -X DELETE http://localhost:11434/api/delete -d '{
  "model": "llama3:13b"
}'
```

#### Response 回复

Returns a 200 OK if successful, 404 Not Found if the model to be deleted doesn't exist.
如果成功则返回 200 OK，如果要删除的模型不存在则返回 404 Not Found。

## Pull a Model 拉模型

```
POST /api/pull
```

Download a model from the ollama library. Cancelled pulls are resumed from where they left off, and multiple calls will share the same download progress.
从 ollama 库下载模型。取消的拉取将从中断处恢复，并且多个调用将共享相同的下载进度。

### Parameters 参数

- `model`: name of the model to pull
  `model` : 要拉取的模型的名称
- `insecure`: (optional) allow insecure connections to the library. Only use this if you are pulling from your own library during development.
  `insecure` ：（可选）允许与库的不安全连接。仅当您在开发期间从自己的库中提取时才使用此选项。
- `stream`: (optional) if `false` the response will be returned as a single response object, rather than a stream of objects
  `stream` ：（可选）如果为`false`响应将作为单个响应对象返回，而不是对象流

### Examples 示例

#### Request 要求

```
curl http://localhost:11434/api/pull -d '{
  "model": "llama3.2"
}'
```

#### Response 回复

If `stream` is not specified, or set to `true`, a stream of JSON objects is returned:
如果未指定`stream`或设置为`true` ，则返回 JSON 对象流：

The first object is the manifest:
第一个对象是清单：

```
{
  "status": "pulling manifest"
}
```

Then there is a series of downloading responses. Until any of the download is completed, the `completed` key may not be included. The number of files to be downloaded depends on the number of layers specified in the manifest.
然后就是一系列的下载响应。在任何下载完成之前，可能不会包含`completed`密钥。要下载的文件数量取决于清单中指定的层数。

```
{
  "status": "downloading digestname",
  "digest": "digestname",
  "total": 2142590208,
  "completed": 241970
}
```

After all the files are downloaded, the final responses are:
所有文件下载完毕后，最终响应为：

```
{
    "status": "verifying sha256 digest"
}
{
    "status": "writing manifest"
}
{
    "status": "removing any unused layers"
}
{
    "status": "success"
}
```

if `stream` is set to false, then the response is a single JSON object:
如果`stream`设置为 false，则响应是单个 JSON 对象：

```
{
  "status": "success"
}
```

## Push a Model 推送模型

```
POST /api/push
```

Upload a model to a model library. Requires registering for ollama.ai and adding a public key first.
将模型上传到模型库。需要先注册 ollama.ai 并添加公钥。

### Parameters 参数

- `model`: name of the model to push in the form of `/:`
  `model` ：要推送的模型名称，格式为`/:`
- `insecure`: (optional) allow insecure connections to the library. Only use this if you are pushing to your library during development.
  `insecure` ：（可选）允许与库的不安全连接。仅当您在开发期间推送到库时才使用此选项。
- `stream`: (optional) if `false` the response will be returned as a single response object, rather than a stream of objects
  `stream` ：（可选）如果为`false`响应将作为单个响应对象返回，而不是对象流

### Examples 示例

#### Request 要求

```
curl http://localhost:11434/api/push -d '{
  "model": "mattw/pygmalion:latest"
}'
```

#### Response 回复

If `stream` is not specified, or set to `true`, a stream of JSON objects is returned:
如果未指定`stream`或设置为`true` ，则返回 JSON 对象流：

```
{ "status": "retrieving manifest" }
```

and then: 进而：

```
{
  "status": "starting upload",
  "digest": "sha256:bc07c81de745696fdf5afca05e065818a8149fb0c77266fb584d9b2cba3711ab",
  "total": 1928429856
}
```

Then there is a series of uploading responses:
然后就是一系列的上传响应：

```
{
  "status": "starting upload",
  "digest": "sha256:bc07c81de745696fdf5afca05e065818a8149fb0c77266fb584d9b2cba3711ab",
  "total": 1928429856
}
```

Finally, when the upload is complete:
最后，上传完成后：

```
{"status":"pushing manifest"}
{"status":"success"}
```

If `stream` is set to `false`, then the response is a single JSON object:
如果`stream`设置为`false` ，则响应是单个 JSON 对象：

```
{ "status": "success" }
```

## Generate Embeddings 生成嵌入

```
POST /api/embed
```

Generate embeddings from a model
从模型生成嵌入

### Parameters 参数

- `model`: name of model to generate embeddings from
  `model` ：生成嵌入的模型名称
- `input`: text or list of text to generate embeddings for
  `input` ：要为其生成嵌入的文本或文本列表

Advanced parameters: 高级参数：

- `truncate`: truncates the end of each input to fit within context length. Returns error if `false` and context length is exceeded. Defaults to `true`
  `truncate` ：截断每个输入的末尾以适应上下文长度。如果为`false`并且超出上下文长度，则返回错误。默认为`true`
- `options`: additional model parameters listed in the documentation for the [Modelfile](https://github.com/ollama/ollama/blob/main/docs/modelfile.md#valid-parameters-and-values) such as `temperature`
  `options` ：[模型](https://github.com/ollama/ollama/blob/main/docs/modelfile.md#valid-parameters-and-values)文件文档中列出的其他模型参数，例如`temperature`
- `keep_alive`: controls how long the model will stay loaded into memory following the request (default: `5m`)
  `keep_alive` ：控制模型在请求后加载到内存中的时间（默认值： `5m` ）

### Examples 示例

#### Request 要求

```
curl http://localhost:11434/api/embed -d '{
  "model": "all-minilm",
  "input": "Why is the sky blue?"
}'
```

#### Response 回复

```
{
  "model": "all-minilm",
  "embeddings": [[
    0.010071029, -0.0017594862, 0.05007221, 0.04692972, 0.054916814,
    0.008599704, 0.105441414, -0.025878139, 0.12958129, 0.031952348
  ]],
  "total_duration": 14143917,
  "load_duration": 1019500,
  "prompt_eval_count": 8
}
```

#### Request (Multiple input) 请求（多输入）

```
curl http://localhost:11434/api/embed -d '{
  "model": "all-minilm",
  "input": ["Why is the sky blue?", "Why is the grass green?"]
}'
```

#### Response 回复

```
{
  "model": "all-minilm",
  "embeddings": [[
    0.010071029, -0.0017594862, 0.05007221, 0.04692972, 0.054916814,
    0.008599704, 0.105441414, -0.025878139, 0.12958129, 0.031952348
  ],[
    -0.0098027075, 0.06042469, 0.025257962, -0.006364387, 0.07272725,
    0.017194884, 0.09032035, -0.051705178, 0.09951512, 0.09072481
  ]]
}
```

## List Running Models 列出运行模型

```
GET /api/ps
```

List models that are currently loaded into memory.
列出当前加载到内存中的模型。

#### Examples 示例

### Request 要求

```
curl http://localhost:11434/api/ps
```

#### Response 回复

A single JSON object will be returned.
将返回单个 JSON 对象。

```
{
  "models": [
    {
      "name": "mistral:latest",
      "model": "mistral:latest",
      "size": 5137025024,
      "digest": "2ae6f6dd7a3dd734790bbbf58b8909a606e0e7e97e94b7604e0aa7ae4490e6d8",
      "details": {
        "parent_model": "",
        "format": "gguf",
        "family": "llama",
        "families": [
          "llama"
        ],
        "parameter_size": "7.2B",
        "quantization_level": "Q4_0"
      },
      "expires_at": "2024-06-04T14:38:31.83753-07:00",
      "size_vram": 5137025024
    }
  ]
}
```

## Generate Embedding 生成嵌入

> Note: this endpoint has been superseded by `/api/embed`
> 注意：此端点已被`/api/embed`取代

```
POST /api/embeddings
```

Generae embeddings from a model
从模型生成嵌入

### Parameters 参数

- `model`: name of model to generate embeddings from
  `model` ：生成嵌入的模型名称
- `prompt`: text to generate embeddings for
  `prompt` ：要为其生成嵌入的文本

Advanced parameters: 高级参数：

- `options`: additional model parameters listed in the documentation for the [Modelfile](https://github.com/ollama/ollama/blob/main/docs/modelfile.md#valid-parameters-and-values) such as `temperature`
  `options` ：[模型](https://github.com/ollama/ollama/blob/main/docs/modelfile.md#valid-parameters-and-values)文件文档中列出的其他模型参数，例如`temperature`
- `keep_alive`: controls how long the model will stay loaded into memory following the request (default: `5m`)
  `keep_alive` ：控制模型在请求后加载到内存中的时间（默认值： `5m` ）

### Examples 示例

#### Request 要求

```
curl http://localhost:11434/api/embeddings -d '{
  "model": "all-minilm",
  "prompt": "Here is an article about llamas..."
}'
```

#### Response 回复

```
{
  "embedding": [
    0.5670403838157654, 0.009260174818336964, 0.23178744316101074, -0.2916173040866852, -0.8924556970596313,
    0.8785552978515625, -0.34576427936553955, 0.5742510557174683, -0.04222835972905159, -0.137906014919281
  ]
}
```