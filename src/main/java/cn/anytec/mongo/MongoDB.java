package cn.anytec.mongo;

import cn.anytec.config.GeneralConfig;
import com.mongodb.MongoClient;
import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.result.UpdateResult;
import org.bson.Document;
import org.bson.conversions.Bson;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import static com.mongodb.client.model.Filters.eq;
import static com.mongodb.client.model.Updates.push;

@Component
public class MongoDB implements MongoDBService {
    @Autowired
    GeneralConfig config;

    private static final Logger logger = LoggerFactory.getLogger(MongoDB.class);
    private final MongoClient mongoClient = new MongoClient("127.0.0.1");
    private final MongoDatabase database = mongoClient.getDatabase("xm");
    private final MongoCollection<Document> waterSlideCollection = database.getCollection("waterSlide");
    private final MongoCollection<Document> bumperCarCollection = database.getCollection("bumperCar");
    private final MongoCollection<Document> toyCarCollection = database.getCollection("toyCar");
    private final MongoCollection<Document> arAreaCollection = database.getCollection("arArea");


    public MongoDB(){
        logger.info("======= 初始化MongoDB =======");
    }

    @Override
    //添加游客Id
    public boolean addVisitorId(String visitorId,String place){
        if(place.equals(config.getWaterSlide())) {
            return addVisitor(waterSlideCollection, visitorId);
        }else {
            return false;
        }
    }

    public boolean addVisitor(MongoCollection<Document> collection,String visitorId){
        FindIterable<Document> documents= collection.find(eq("visitor_id",visitorId));
        if (null!=documents.first()){
            logger.warn("游客已存在");
            return false;
        }
        collection.insertOne(new Document("visitor_id",visitorId));
        return true;
    }

    @Override
    //移除游客Id
    public boolean removeVisitorId(String visitorId,String place){
        if(place.equals(config.getWaterSlide())){
            waterSlideCollection.deleteOne(eq("visitor_id",visitorId));
            return true;
            } else if(place.equals(config.getBumperCar())){
            bumperCarCollection.deleteOne(eq("visitor_id",visitorId));
            return true;
        }else if(place.equals(config.getToyCar())){
            toyCarCollection.deleteOne(eq("visitor_id",visitorId));
            return true;
        }else if(place.equals(config.getArArea())){
            arAreaCollection.deleteOne(eq("visitor_id",visitorId));
            return true;
        }else {
            return false;
        }
    }

//    @Override
//    public int addVideoToVisitor(String visitorId, String videoUrl){
//        Document document = waterSlideCollection.find(eq("visitor_id", visitorId)).first();
//        if(document == null){
//            logger.warn("id不存在");
//            return 0;
//        }
//        Bson filter = eq("visitor_id", visitorId);
//        Bson change = push("videos", videoUrl);
//        waterSlideCollection.updateOne(filter, change);
//        return 1;
//    }
//
//    @Override
//    public int deleteVideoById(String visitorId, String videoUrl){
//        Document document = waterSlideCollection.find(eq("visitor_id", visitorId)).first();
//        if(document == null){
//            logger.warn("id不存在");
//            return 0;
//        }
//        List<String> videoList = (List)document.get("videos");
//        if(!document.containsKey("videos")||videoList.size() < 1){
//            logger.warn("指定id无视频可删除");
//            return 0;
//        }
//        Iterator<String> iterator = videoList.iterator();
//        while (iterator.hasNext()){
//            String video = iterator.next();
//            if(video.equals(videoUrl))
//                iterator.remove();
//        }
//        Bson filter = eq("visitor_id", visitorId);
//        waterSlideCollection.replaceOne(filter,document);
//        return 1;
//    }


    @Override
    //获取视频地址列表
    public List<String> getVideoUrlList(String visitorId,String place){
        if(place.equals(config.getWaterSlide())){
            return getVideo(waterSlideCollection,visitorId);
        } else if(place.equals(config.getBumperCar())){
            return getVideo(bumperCarCollection,visitorId);
        }else if(place.equals(config.getToyCar())){
            return getVideo(toyCarCollection,visitorId);
        }else if(place.equals(config.getArArea())){
            return getVideo(arAreaCollection,visitorId);
        }else {
            return Collections.emptyList();
        }
    }

    public List<String> getVideo(MongoCollection<Document> collection,String visitorId){
        List<String> videoUrlList = new ArrayList<>();
        Document document= collection.find(eq("visitor_id",visitorId)).first();
        if(null==document||!document.containsKey("videourl_list")){
            return Collections.emptyList();
        }
        videoUrlList = (List<String>) document.get("videourl_list");
        return videoUrlList;
    }

    @Override
    //视频地址入库
    public boolean saveVideoUrlList(String visitorId, String place,List<String> videoUrlList){
        if(place.equals(config.getWaterSlide())){
            saveVideo(waterSlideCollection,visitorId,videoUrlList);
            return true;
        } else if(place.equals(config.getBumperCar())){
            saveVideo(bumperCarCollection,visitorId,videoUrlList);
            return true;
        }else if(place.equals(config.getToyCar())){
            saveVideo(toyCarCollection,visitorId,videoUrlList);
            return true;
        }else if(place.equals(config.getArArea())){
            saveVideo(arAreaCollection,visitorId,videoUrlList);
            return true;
        }else {
            return false;
        }
    }

    public void saveVideo(MongoCollection<Document> collection,String visitorId,List<String> videoUrlList){
        Document document = collection.find(eq("visitor_id", visitorId)).first();
        if(document == null){
            collection.insertOne(new Document("visitor_id",visitorId).append("videourl_list",videoUrlList));
        }
        else {
            List<String> videourl_list = (List<String>) document.get("videourl_list");
            videourl_list.addAll(videoUrlList);
            Document filter = new Document();
            filter.append("visitor_id", visitorId);
            Document updateDocument = new Document();
            updateDocument.append("$set",new Document("videourl_list",videourl_list));
            collection.updateOne(filter,updateDocument);
        }
    }


}
