package com.linkedin.thirdeye.datalayer.util;

import com.google.common.collect.BiMap;
import com.linkedin.thirdeye.datalayer.entity.AbstractJsonEntity;
import com.linkedin.thirdeye.db.entity.AnomalyTaskSpec;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.lang.reflect.Field;
import java.sql.Clob;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.Timestamp;
import java.sql.Types;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.node.ObjectNode;
import org.modelmapper.ModelMapper;
import org.modelmapper.config.Configuration.AccessLevel;
import org.modelmapper.convention.NameTokenizers;

public class GenericResultSetMapper {

  ModelMapper modelMapper = new ModelMapper();
  private EntityMappingHolder entityMappingHolder;

  {
    modelMapper.getConfiguration().setSourceNameTokenizer(NameTokenizers.CAMEL_CASE)
        .setFieldMatchingEnabled(true).setFieldAccessLevel(AccessLevel.PRIVATE);
    //    modelMapper.getConfiguration().setSourceNameTokenizer(NameTokenizers.UNDERSCORE)
    //        .setMatchingStrategy(MatchingStrategies.LOOSE)
    //        .setDestinationNameTokenizer(NameTokenizers.UNDERSCORE);
  }

  public GenericResultSetMapper(EntityMappingHolder entityMappingHolder) {
    this.entityMappingHolder = entityMappingHolder;
  }

  public AbstractJsonEntity mapSingle(ResultSet rs, Class<? extends AbstractJsonEntity> entityClass)
      throws Exception {
    List<AbstractJsonEntity> resultMapList = toEntityList(rs, entityClass);
    if (resultMapList.size() > 0) {
      return resultMapList.get(0);
    }
    return null;
  }

  public List<AbstractJsonEntity> mapAll(ResultSet rs,
      Class<? extends AbstractJsonEntity> entityClass) throws Exception {
    return toEntityList(rs, entityClass);
  }



  private List<AbstractJsonEntity> toEntityList(ResultSet rs,
      Class<? extends AbstractJsonEntity> entityClass) throws Exception {
    String tableName =
        entityMappingHolder.tableToEntityNameMap.inverse().get(entityClass.getSimpleName());
    LinkedHashMap<String, ColumnInfo> columnInfoMap =
        entityMappingHolder.columnInfoPerTable.get(tableName);
    List<AbstractJsonEntity> entityList = new ArrayList<>();

    ObjectMapper mapper = new ObjectMapper();
    while (rs.next()) {
      AbstractJsonEntity entityObj = entityClass.newInstance();
      ResultSetMetaData resultSetMetaData = rs.getMetaData();
      int numColumns = resultSetMetaData.getColumnCount();
      ObjectNode objectNode = mapper.createObjectNode();
      for (int i = 1; i <= numColumns; i++) {
        String dbColumnName = resultSetMetaData.getColumnLabel(i).toLowerCase();
        ColumnInfo columnInfo = columnInfoMap.get(dbColumnName);
        Field field = columnInfo.field;
        Object val;
        if (columnInfo.sqlType == Types.CLOB) {
          Clob clob = rs.getClob(i);
          val = clob.getSubString(1, (int) clob.length());
        } else {
          val = rs.getObject(i);
        }
        if (val == null) {
          continue;
        }
        if (field.getType().isAssignableFrom(Timestamp.class)) {
          objectNode.put(field.getName(), ((Timestamp) val).getTime());
        } else {
          objectNode.put(field.getName(), val.toString());
        }

        /*
         * if (Enum.class.isAssignableFrom(field.getType())) { field.set(entityObj,
         * Enum.valueOf(field.getType().asSubclass(Enum.class), val.toString())); } else if
         * (String.class.isAssignableFrom(field.getType())) { field.set(entityObj, val.toString());
         * } else if (Integer.class.isAssignableFrom(field.getType())) { field.set(entityObj,
         * Integer.valueOf(val.toString())); } else if
         * (Long.class.isAssignableFrom(field.getType())) { field.set(entityObj,
         * Long.valueOf(val.toString())); } else { field.set(entityObj, val); }
         */
      }
      entityObj = mapper.readValue(objectNode, entityClass);
      entityList.add(entityObj);
    }

    return entityList;

  }

  public AbstractJsonEntity mapSingleOLD(ResultSet rs,
      Class<? extends AbstractJsonEntity> entityClass) throws Exception {
    List<Map<String, Object>> resultMapList = toResultMapList(rs, entityClass);
    if (resultMapList.size() > 0) {
      Map<String, Object> map = resultMapList.get(0);
      if (map.get("workerId") != null) {
        ObjectOutputStream oos = new ObjectOutputStream(
            new FileOutputStream(new File("/tmp/map.out." + System.currentTimeMillis())));
        oos.writeObject(map);
        oos.close();
        System.out.println(map);
      }
      AbstractJsonEntity entity = modelMapper.map(map, entityClass);
      System.out.println(entity);
      return entity;
    }
    return null;
  }

  public List<AbstractJsonEntity> mapAllOLD(ResultSet rs,
      Class<? extends AbstractJsonEntity> entityClass) throws Exception {
    List<Map<String, Object>> resultMapList = toResultMapList(rs, entityClass);
    List<AbstractJsonEntity> resultEntityList = new ArrayList<>();
    if (resultMapList.size() > 0) {
      for (Map<String, Object> map : resultMapList) {
        AbstractJsonEntity entity = modelMapper.map(map, entityClass);
        resultEntityList.add(entity);
      }
    }
    return resultEntityList;
  }

  List<Map<String, Object>> toResultMapList(ResultSet rs,
      Class<? extends AbstractJsonEntity> entityClass) throws Exception {
    List<Map<String, Object>> resultMapList = new ArrayList<>();
    String tableName =
        entityMappingHolder.tableToEntityNameMap.inverse().get(entityClass.getSimpleName());
    BiMap<String, String> dbNameToEntityNameMapping =
        entityMappingHolder.columnMappingPerTable.get(tableName);
    while (rs.next()) {
      ResultSetMetaData resultSetMetaData = rs.getMetaData();
      int numColumns = resultSetMetaData.getColumnCount();
      HashMap<String, Object> map = new HashMap<>();
      for (int i = 1; i <= numColumns; i++) {
        String dbColumnName = resultSetMetaData.getColumnLabel(i).toLowerCase();
        String entityFieldName = dbNameToEntityNameMapping.get(dbColumnName);
        Object val = rs.getObject(i);
        if (val != null) {
          map.put(entityFieldName, val.toString());
        }
      }
      resultMapList.add(map);
    }
    System.out.println(resultMapList);
    return resultMapList;
  }

  public static void main(String[] args) throws Exception {
    ModelMapper mapper = new ModelMapper();
    Map<String, Object> result = new HashMap<>();
    //[{jobName=Test_Anomaly_Task, jobId=1, workerId=1, taskType=MONITOR, id=1, taskInfo=clob2: '{"jobExecutionId":1,"monitorType":"UPDATE","expireDaysAgo":0}', lastModified=2016-08-24 17:25:53.258, version=0, taskStartTime=1470356753227, status=RUNNING, taskEndTime=1471220753227}]

    result.put("jobName", "Test_Anomaly_Task");
    result.put("jobId", 1L);
    result.put("taskType", "MONITOR");
    result.put("id", 1L);
    result.put("taskInfo",
        "clob2: '{\"jobExecutionId\":1,\"monitorType\":\"UPDATE\",\"expireDaysAgo\":0}'");
    result.put("taskType", "MONITOR");
    result.put("lastModified", "2016-08-24 17:25:53.258");
    result.put("status", "RUNNING");
    result.put("lastModified", "2016-08-24 17:25:53.258");
    AnomalyTaskSpec taskSpec1 = mapper.map(result, AnomalyTaskSpec.class);
    System.out.println(taskSpec1);

    //INPUT 2
    ObjectInputStream ois =
        new ObjectInputStream(new FileInputStream(new File("/tmp/map.out.1472093046128")));
    Map<String, Object> inputMap = (Map<String, Object>) ois.readObject();
    AnomalyTaskSpec taskSpec2 = mapper.map(inputMap, AnomalyTaskSpec.class);
    System.out.println(taskSpec2);
  }
}
