package org.example.consultant.config;


import dev.langchain4j.community.store.embedding.redis.RedisEmbeddingStore;
import dev.langchain4j.data.document.DocumentSplitter;
import dev.langchain4j.data.document.loader.ClassPathDocumentLoader;
import dev.langchain4j.data.document.parser.apache.pdfbox.ApachePdfBoxDocumentParser;
import dev.langchain4j.data.document.splitter.DocumentSplitters;
import dev.langchain4j.mcp.client.DefaultMcpClient;
import dev.langchain4j.mcp.client.McpClient;
import dev.langchain4j.mcp.client.transport.McpTransport;
import dev.langchain4j.mcp.client.transport.http.HttpMcpTransport;
import dev.langchain4j.memory.ChatMemory;
import dev.langchain4j.memory.chat.ChatMemoryProvider;
import dev.langchain4j.memory.chat.MessageWindowChatMemory;
import dev.langchain4j.model.chat.ChatModel;
import dev.langchain4j.model.embedding.EmbeddingModel;
import dev.langchain4j.model.openai.OpenAiChatModel;
import dev.langchain4j.data.document.Document;
import dev.langchain4j.model.openai.OpenAiStreamingChatModel;
import dev.langchain4j.rag.content.retriever.ContentRetriever;
import dev.langchain4j.rag.content.retriever.EmbeddingStoreContentRetriever;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.store.embedding.EmbeddingStore;
import dev.langchain4j.store.embedding.EmbeddingStoreIngestor;
import dev.langchain4j.store.embedding.inmemory.InMemoryEmbeddingStore;
import dev.langchain4j.store.memory.chat.ChatMemoryStore;
import org.example.consultant.aiservice.ConsultantService;
import org.example.consultant.tools.ReservationTool;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import dev.langchain4j.mcp.McpToolProvider;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

// 使用AiServices工具类创建接口（封装聊天方法）的动态代理对象
@Configuration
public class CommonConfig {
    @Autowired
    private OpenAiChatModel openAiChatModel;
    @Autowired
    private OpenAiStreamingChatModel openAiStreamingChatModel;
    @Autowired
    private ChatMemoryStore redisChatMemoryStore;
    @Autowired
    private EmbeddingModel  embeddingModel;
    @Autowired
    private RedisEmbeddingStore redisEmbeddingStore;
    @Autowired
    private ReservationTool reservationTool;

//    AiServicesspring提供的工具类
//    注入生成的代理对象cs
    @Bean
    public ConsultantService consultantService(
               ChatMemory chatMemory,
               ChatMemoryProvider chatMemoryProvider,
               McpToolProvider mcpToolProvider,
               ContentRetriever contentRetriever) {
        return AiServices.builder(ConsultantService.class)
                .chatModel(openAiChatModel)//设置对话时使用的模型对象
                .streamingChatModel(openAiStreamingChatModel)
                .chatMemory(chatMemory)
                .chatMemoryProvider(chatMemoryProvider)
                .contentRetriever(contentRetriever)
                .tools(reservationTool)
                .toolProvider(mcpToolProvider)
                .build();
    }
    //构建会话记忆对象
    @Bean
    public ChatMemory chatMemory() {
        MessageWindowChatMemory memory = MessageWindowChatMemory.builder()
                .maxMessages(20)
                .build();
        return memory;
    }
    //构建ChatMemoryProvider对象
    @Bean
    public ChatMemoryProvider  chatMemoryProvider() {
        ChatMemoryProvider chatMemoryProvider = new ChatMemoryProvider() {
            @Override
            public ChatMemory get(Object memoryId) {
                return MessageWindowChatMemory.builder()
                         .id(memoryId)
                         .maxMessages(20)
                        .chatMemoryStore(redisChatMemoryStore)
                         .build();
            }
        };
        return chatMemoryProvider;
    }

    //构建向量数据库操作对象
    //使得分段、向量化、存储到redis向量数据库执行一次
   // @Bean
    public EmbeddingStore store(){
        //1.加载文档进内存
//        List<Document> documents = ClassPathDocumentLoader.loadDocuments("content");
        List<Document> documents = ClassPathDocumentLoader.loadDocuments("content",new ApachePdfBoxDocumentParser());
        //2.构建向量数据库操作对象 内存版本向量数据库
        //InMemoryEmbeddingStore store = new InMemoryEmbeddingStore();
        //构建文档分割器对象
        DocumentSplitter ds = DocumentSplitters.recursive(500,100);
        //3.构建一个EmbeddingStoreIngestor对象,完成文本数据切割,向量化, 存储
        EmbeddingStoreIngestor ingestor = EmbeddingStoreIngestor.builder()
                //.embeddingModel(store)
                .embeddingStore(redisEmbeddingStore)
                .documentSplitter(ds)
                .embeddingModel(embeddingModel)
                .build();
        ingestor.ingest(documents);
        return redisEmbeddingStore;
    }

    //构建向量数据库检索对象
    @Bean
//    public ContentRetriever contentRetriever(EmbeddingStore store){
    public ContentRetriever contentRetriever() {
        return EmbeddingStoreContentRetriever.builder()
                //.embeddingStore(store)//设置向量数据库操作对象
                .embeddingStore(redisEmbeddingStore)
                .minScore(0.5)//设置最小分数
                .maxResults(3)//设置最大片段数量
                .embeddingModel(embeddingModel)
                .build();
    }


    @Bean// 👉 显式命名，和 AiService 对应
    public McpToolProvider mcpToolProvider() {
//        Map<String, String> headers = new HashMap<>();
//        headers.put("Authorization", "05c1a011231646ed91a723b167a8ff36.kX1pnibXjCmBlJA9");
        //和MCP服务通信
//        McpTransport transport = new StreamableHttpMcpTransport.Builder()
//                .url("https://open.bigmodel.cn/api/mcp/web_search/sse")
//                .logRequests(true) // 打印请求
//                .logResponses(true) // 打印响应
//                .build();
        McpTransport transport = new HttpMcpTransport.Builder()
                .sseUrl("https://open.bigmodel.cn/api/mcp/web_search/sse?Authorization=05c1a011231646ed91a723b167a8ff36.kX1pnibXjCmBlJA9")
                .logRequests(true)
                .logResponses(true)
                .build();
        //创建MCP客户端对象
        McpClient mcpClient = new DefaultMcpClient.Builder()
                .key("MyMCPClient")
                .transport(transport)
                .build();
        //从MCP获取工具
        return McpToolProvider.builder()
                .mcpClients(List.of(mcpClient))
                .build();
    }
}
