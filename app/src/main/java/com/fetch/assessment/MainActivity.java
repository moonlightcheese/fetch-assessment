package com.fetch.assessment;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.view.LayoutInflater;

import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import androidx.constraintlayout.widget.ConstraintLayout;
import com.fetch.HttpEventListener;
import com.fetch.HttpRequestThread;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends Activity {
    public static final class JSON {
        public static final String ID = "id";
        public static final String LISTID = "listId";
        public static final String NAME = "name";
    }
    private View view;
    ExpandableListView list;
    LayoutInflater inflater;
    public static final String URL = "https://fetch-hiring.s3.amazonaws.com/hiring.json";
    private final Logger logger = LoggerFactory.getLogger(this.getClass());
    private JSONArray data = null;
    private class JsonAdapter extends SimpleExpandableListAdapter {
        private static final int GROUP_LAYOUT_RES_ID = android.R.layout.simple_expandable_list_item_1;
        private static final int CHILD_LAYOUT_RES_ID = android.R.layout.simple_list_item_2;

        public JsonAdapter(
                Context context,
                List<? extends Map<String, ?>> groupData,
                List<? extends List<? extends Map<String, ?>>> childData) {
            super(
                    context,
                    groupData,
                    GROUP_LAYOUT_RES_ID,
                    new String[] {
                            "listId"
                    },
                    new int[] {
                            android.R.id.text1
                    },
                    childData,
                    CHILD_LAYOUT_RES_ID,
                    new String[] {
                            "name",
                            "id"
                    },
                    new int[] {
                            android.R.id.text1,
                            android.R.id.text2
                    });
        }

//        @Override
//        public View getView(
//                int position,
//                View convertView,
//                ViewGroup parent) {
//            if(convertView == null)
//                convertView = inflater.inflate(R.layout.main_list_item, parent);
//
//            TextView idView = convertView.findViewById(R.id.id);
//            TextView listIdView = convertView.findViewById(R.id.list_id);
//            TextView nameView = convertView.findViewById(R.id.name);
//
//            JSONObject itemData = null;
//            try {
//                itemData = data.getJSONObject(position);
//            } catch(JSONException je) {
//                logger.warn("Unable to create JSONObject!", je);
//            }
//
//            try {
//                int id = itemData.getInt(JSON.ID);
//                idView.setText(String.valueOf(id));
//            } catch(JSONException je) {
//                logger.warn("Unable to get 'id' field!", je);
//            }
//
//            try {
//                int listId = itemData.getInt(JSON.LISTID);
//                listIdView.setText(String.valueOf(listId));
//            } catch(JSONException je) {
//                logger.warn("Unable to get 'listId' field!", je);
//            }
//
//            try {
//                String name = itemData.getString(JSON.NAME);
//                nameView.setText(name);
//            } catch(JSONException je) {
//                logger.warn("Unable to get 'name' field!", je);
//            }
//
//            return convertView;
//        }
    }
    private HttpEventListener fetchListener = new HttpEventListener() {
        @Override
        public void onResponseReceived(String response) {
            try {
                data = new JSONArray(response);
                quicksort(data, 0, data.length()-1);
            } catch(JSONException je) {
                logger.warn("Unable to parse JSON array!", je);
            }

            if(data == null || data.length() <= 0) {
                logger.warn("JSON array is null!");
                Toast.makeText(
                        MainActivity.this,
                        "JSON array was empty or null",
                        Toast.LENGTH_LONG);
                return;
            }

            //TODO: sort results
            try {
                List<Map<String, String>> groupData = new ArrayList<>();
                List<List<Map<String, String>>> childData = new ArrayList<>();
                for(int i = 0; i < data.length(); i++) {
                    JSONObject jsonObject = data.getJSONObject(i);

                    //filter out blank or null names
                    if(jsonObject.getString(JSON.NAME) == null ||
                            jsonObject.getString(JSON.NAME).equals("") ||
                            jsonObject.getString(JSON.NAME).equals("null"))
                        continue;

                    //check if the listId is already in the group list, if not, add it
                    int g = 0;
                    Integer jsonListId = jsonObject.getInt(JSON.LISTID);
                    for(; g < groupData.size(); g++) {
                        String listId = groupData.get(g).get(JSON.LISTID);
                        if(jsonListId != null) {
                            if(listId == String.valueOf(jsonListId)) {
                                break;
                            }
                        } else if(jsonListId == null && listId == null) {
                            break;
                        }
                    }

                    if(g == groupData.size()) {
                        //if 'g' is equal to the last index of groupData, we should add a new Map
                        Map newGroup = new HashMap<String, String>();
                        if(jsonListId != null) {
                            newGroup.put(JSON.LISTID, String.valueOf(jsonListId));
                        } else {
                            newGroup.put(JSON.LISTID, null);
                        }
                        groupData.add(newGroup);
                        childData.add(new ArrayList<>());
                    }

                    //we must also add an entry in the childData
                    Map newChild = new HashMap<String, String>();
                    Integer jsonId = jsonObject.getInt(JSON.ID);
                    if(jsonId != null) { //avoid NPE
                        newChild.put(JSON.ID, String.valueOf(jsonId));
                    } else {
                        newChild.put(JSON.ID, null);
                    }
                    newChild.put(JSON.NAME, jsonObject.getString(JSON.NAME));
                    childData.get(g).add(newChild);
                }

                Message msg = new Message();
                msg.obj = new JsonAdapter(
                        MainActivity.this,
                        groupData,
                        childData);
                handler.sendMessage(msg);
            } catch(JSONException je) {
                logger.warn("Unable to parse JSON array!", je);
            }
        }

        @Override
        public void onError(String message, Exception e) {
            Toast.makeText(
                    MainActivity.this,
                    message,
                    Toast.LENGTH_LONG);
        }
    };

    @SuppressLint("HandlerLeak")
    protected Handler handler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            list.setAdapter((JsonAdapter)msg.obj);
        }
    };

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflater = (LayoutInflater)getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        view = inflater.inflate(R.layout.main, null);
        setContentView(view);

        list = view.findViewById(R.id.main_list);

        Button fetchButton = view.findViewById(R.id.fetch_button);
        fetchButton.setOnClickListener(v -> {
            try {
                HttpRequestThread requestThread = new HttpRequestThread(
                        new URL(URL),
                        fetchListener);
                requestThread.start();
            } catch(MalformedURLException mue) {
                logger.warn("Malformed URL!", mue);
            }
        });
    }

    private void quicksort(JSONArray records, final int start, final int end) throws JSONException {
        if(start > 900 || end > 900)
            logger.warn("start = " + start + "   end = " + end + "   size = " + records.length());
        // index for the "left-to-right scan"
        int i = start;
        // index for the "right-to-left scan"
        int j = end;

        // only examine arrays of 2 or more elements.
        if (j - i >= 1) {
            // The pivot point of the sort method is arbitrarily set to the first element in the array.
            JSONObject pivot = records.getJSONObject(i);
            // only scan between the two indexes, until they meet.
            while (j > i) {
                // from the left, if the current element is lexicographically less than the (original)
                // first element in the String array, move on. Stop advancing the counter when we reach
                // the right or an element that is lexicographically greater than the pivot String.
                while (compare(records.getJSONObject(i), pivot) <= 0 && i < end && j > i) {
                    i++;
                }
                // from the right, if the current element is lexicographically greater than the (original)
                // first element in the String array, move on. Stop advancing the counter when we reach
                // the left or an element that is lexicographically less than the pivot String.
                while (compare(records.getJSONObject(j), pivot) >= 0 && j > start && j >= i) {
                    j--;
                }
                // check the two elements in the center, the last comparison before the scans cross.
                if (j > i)
                    swap(records, i, j);
            }
            // At this point, the two scans have crossed each other in the center of the array and stop.
            // The left partition and right partition contain the right groups of numbers but are not
            // sorted themselves. The following recursive code sorts the left and right partitions.

            // Swap the pivot point with the last element of the left partition.
            swap(records, start, j);
            // sort left partition
            quicksort(records, start, j - 1);
            // sort right partition
            quicksort(records, j + 1, end);
        }
    }
    /**
     * This method facilitates the quickSort method's need to swap two elements, Towers of Hanoi style.
     */
    private static void swap(JSONArray records, int i, int j) throws JSONException {
        JSONObject tmp = records.getJSONObject(i);
        JSONObject swap = records.getJSONObject(j);
        records.put(i, swap);
        records.put(j, tmp);
    }

    /**
     * order by listId then name in comparator
     * @param left
     * @param right
     * @return
     * @throws JSONException
     */
    public int compare(JSONObject left, JSONObject right) throws JSONException {
        int cmp = left.getString(JSON.LISTID).compareTo(right.getString(JSON.LISTID));
        if(cmp == 0)
            cmp = left.getString(JSON.NAME).compareTo(right.getString(JSON.NAME));
        return cmp;
    }
}
