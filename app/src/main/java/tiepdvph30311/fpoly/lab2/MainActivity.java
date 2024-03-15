package tiepdvph30311.fpoly.lab2;

import android.app.Dialog;
import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import okhttp3.OkHttpClient;
import tiepdvph30311.fpoly.lab2.Adapter.ItemAdapter;
import tiepdvph30311.fpoly.lab2.Modal.Item;

public class MainActivity extends AppCompatActivity {
    ExecutorService service;

    public OkHttpClient client = new OkHttpClient();
    public String apAddress = "http://10.24.25.124:9999";

    RecyclerView listItem;
    FloatingActionButton fabAdd;
    ItemAdapter adapter;
    List<Item> list;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        service = Executors.newCachedThreadPool();
        listItem = findViewById(R.id.listItem);
        fabAdd = findViewById(R.id.fab);
        list = new ArrayList<>();

        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        listItem.setLayoutManager(layoutManager);

        fabAdd.setOnClickListener(view -> {
            showAddDialog();
        });

        GetData();
    }

    private JSONArray callAPIGetData(String urlString) throws Exception {
        // Tạo URL từ đường dẫn đã cho
        URL url = new URL(urlString);
        // Mở kết nối
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            // Đọc dữ liệu từ kết nối
            InputStream in = urlConnection.getInputStream();
            BufferedReader reader = new BufferedReader(new InputStreamReader(in));
            StringBuilder result = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                result.append(line);
            }
            // Chuyển dữ liệu đọc được thành một JSONArray
            return new JSONArray(result.toString());
        } finally {
            // Đóng kết nối
            urlConnection.disconnect();
        }
    }
    public interface ResponseListener {
        void onResponse(String response);

        void onError(Exception e);
    }
    public void GetData() {
        try {
            Future<JSONArray> future = service.submit(new MyCallable());
            if (future.get() != null) {
                JSONArray jsonArray = new JSONArray(future.get().toString());
                for (int i = 0; i < jsonArray.length(); i++) {
                    JSONObject jsonObject = jsonArray.getJSONObject(i);
                    String id = jsonObject.getString("_id");
                    String name = jsonObject.getString("name");
                    int price = jsonObject.getInt("price");
                    String brand = jsonObject.getString("brand");

                    Item product = new Item(id, name, price, brand);
                    list.add(product);
                }
                adapter = new ItemAdapter(list, MainActivity.this);
                listItem.setAdapter(adapter);
            }
        } catch (Exception e) {
            Log.e("GetData", "Error: " + e.getMessage());
        }
    }
    public void postData(String name, int price, String brand) {
        service.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    // Tạo URL
                    URL url = new URL(apAddress + "/product/post");
                    // Mở kết nối
                    HttpURLConnection conn = (HttpURLConnection) url.openConnection();
                    conn.setRequestMethod("POST");
                    conn.setRequestProperty("Content-Type", "application/json");
                    conn.setDoOutput(true);

                    // Tạo đối tượng JSON
                    JSONObject postData = new JSONObject();
                    postData.put("name", name);
                    postData.put("price", price);
                    postData.put("brand", brand);

                    // Ghi dữ liệu JSON vào luồng đầu ra
                    conn.getOutputStream().write(postData.toString().getBytes());

                    // Đọc phản hồi từ máy chủ
                    BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        response.append(line);
                    }

                    // Đóng kết nối
                    conn.disconnect();

                    // Hiển thị phản hồi
                    Log.d("POST Response", response.toString());

                    // Nếu cần xử lý phản hồi ở đây

                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        });
    }


    public void showAddDialog() {
        Dialog dialog = new Dialog(this);
        dialog.setContentView(R.layout.dialog_item);

        EditText editTextName = dialog.findViewById(R.id.editTextName);
        EditText editTextPrice = dialog.findViewById(R.id.editTextPrice);
        EditText editTextBrand = dialog.findViewById(R.id.editTextBrand);

        Button buttonAdd = dialog.findViewById(R.id.buttonAdd);
        Button buttonCancel = dialog.findViewById(R.id.buttonCancel);

        buttonAdd.setOnClickListener(v -> {
            String name = editTextName.getText().toString();
            int price = Integer.parseInt(editTextPrice.getText().toString());
            String brand = editTextBrand.getText().toString();

            // Thêm logic để gửi dữ liệu lên server ở đây
            postData(name, price, brand);
            // Sau khi gửi dữ liệu thành công, cập nhật danh sách và adapter
            list.add(new Item("temp_id", name, price, brand));
            adapter.notifyDataSetChanged();

            Toast.makeText(this, "Thêm sản phẩm thành công", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
        });

        buttonCancel.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    public class MyCallable implements Callable<JSONArray> {

        @Override
        public JSONArray call() throws Exception {
            JSONArray jsonArray = callAPIGetData(apAddress + "/product/getall");
            return jsonArray;
        }
    }
}
