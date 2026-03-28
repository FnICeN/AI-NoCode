package com.nocode.backend.service.impl;

import com.mybatisflex.spring.service.impl.ServiceImpl;
import com.nocode.backend.model.entity.App;
import com.nocode.backend.mapper.AppMapper;
import com.nocode.backend.service.AppService;
import org.springframework.stereotype.Service;

/**
 * 应用 服务层实现。
 *
 * @author FICN
 */
@Service
public class AppServiceImpl extends ServiceImpl<AppMapper, App>  implements AppService{

}
