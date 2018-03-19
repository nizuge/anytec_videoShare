package cn.anytec.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.ViewControllerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurerAdapter;
@Configuration
public class MVCConfiguration extends WebMvcConfigurerAdapter {

    @Autowired
    GeneralConfig config;

    //请求直接映射页面
    @Override
    public void addViewControllers(ViewControllerRegistry registry) {
        //registry.addViewController("/login").setViewName("login");
    }

    //静态资源映射
    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        //相对路径
        //registry.addResourceHandler("/static/**").addResourceLocations("classpath:/source/");
        //绝对路径
        registry.addResourceHandler("/anytec/videos/**").addResourceLocations("file:"+ config.getVideoSavePath());
        super.addResourceHandlers(registry);
        //绝对路径
        registry.addResourceHandler("/anytec/areaVideos/**").addResourceLocations("file:"+ config.getAreaVideoPath());
        super.addResourceHandlers(registry);
    }
}
