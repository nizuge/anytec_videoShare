package cn.anytec.mongo;

import org.springframework.stereotype.Service;

import java.util.List;
@Service
public interface MongoDBService {

    boolean addVisitorId(String visitorId,String place);

    boolean removeVisitorId(String visitorId,String place);

    List<String> getVideoUrlList(String visitorId,String place);

//    boolean deleteVideoById(String visitorId, String videoUrl);
//
//    boolean addAreaVisitorId(String visitorId);

    boolean saveVideoUrlList(String visitorId, String place,List<String> videoUrlList);

}
