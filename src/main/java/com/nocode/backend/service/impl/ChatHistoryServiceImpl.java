package com.nocode.backend.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nocode.backend.model.entity.ChatHistory;
import com.nocode.backend.mapper.ChatHistoryMapper;
import com.nocode.backend.service.ChatHistoryService;
import org.springframework.stereotype.Service;

/**
 * 对话历史 服务层实现。
 *
 * @author FICN
 */
@Service
public class ChatHistoryServiceImpl extends ServiceImpl<ChatHistoryMapper, ChatHistory>  implements ChatHistoryService{

}
