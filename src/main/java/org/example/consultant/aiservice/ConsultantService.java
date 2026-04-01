package org.example.consultant.aiservice;

import dev.langchain4j.service.MemoryId;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import dev.langchain4j.service.spring.AiService;
import dev.langchain4j.service.spring.AiServiceWiringMode;
import reactor.core.publisher.Flux;

//@AiService(
//        wiringMode = AiServiceWiringMode.EXPLICIT, //手动装配
//        streamingChatModel = "openAiStreamingChatModel",
//        chatModel = "openAiChatModel",
//        chatMemory = "chatMemory",//配置绘画记忆功能
//        chatMemoryProvider = "chatMemoryProvider" ,//配置会话记忆提供者
//        contentRetriever = "contentRetriever",
//        tools = {"reservationTool", "mcpToolProvider"}
//)
//@AiService
//public interface ConsultantService {
//    //用于聊天的方法,message为用户输入的内容
//    public String chat(String message);
//}
public interface ConsultantService {
    //用于聊天的方法,message为用户输入的内容
    //@SystemMessage("你是志愿填报助手小愿！只回答志愿填报相关信息")
    @SystemMessage(fromResource = "system.txt")
    public Flux<String> chat(@MemoryId String memoryId, @UserMessage String message);
}
