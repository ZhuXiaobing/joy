package org.joy;

import com.alibaba.fastjson.JSONArray;
import org.apache.http.HttpHeaders;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;


public class AllStockInfo {
    public static void main(String[] args) throws Exception {
        new AllStockInfo().getAllStockByType("sh_a", 1);
        new AllStockInfo().getAllStockByType("sz_a", 1);
    }

    /**
     * 获取指定类型的所有股票信息（单次查询最多返回100只股票）
     *
     * @param stockType 股票类型（深圳A股：sz_a  深圳B股：sz_a  上海A股:sh_a）
     * @return 返回所有符合类型的股票信息
     */
    public void getAllStockByType(String stockType, int pageNum) throws Exception {
        String url = "http://vip.stock.finance.sina.com.cn/quotes_service/api/json_v2.php/Market_Center.getHQNodeData?" +
                "page=" + pageNum +
                "&num=100" +
                "&sort=symbol" +//排序的列
                "&asc=1" +//1：升序  0：降序
                "&node=" + stockType +
                "&symbol=" +
                "&_s_r_a=init";//以symbol升序时 默认为init 根据其他排序时均为sort
        //发送请求
        HttpClient client = HttpClients.createDefault();
        HttpGet request = new HttpGet(url);
        request.setHeader(HttpHeaders.REFERER, "http://finance.sina.com.cn");
        HttpResponse response = client.execute(request);
        String resultStr = EntityUtils.toString(response.getEntity());
        JSONArray resultJson = JSONArray.parseArray(resultStr);
        System.out.println("No." + pageNum+ " "+stockType + " 总共有 " + resultJson.size() + " 只股票");
        if (resultJson.size() == 100) {
            getAllStockByType(stockType, pageNum+1);
        }
    }

}
