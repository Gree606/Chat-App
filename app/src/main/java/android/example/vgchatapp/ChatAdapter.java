package android.example.vgchatapp;

import android.content.Context;
import android.graphics.Color;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;

import androidx.annotation.NonNull;

public class ChatAdapter extends ArrayAdapter<String> {
    ArrayList<Boolean> isUser = new ArrayList<>();

    public ChatAdapter(Context context, int list_item,int id) {
        super(context, 0, id);
    }

    public void setIsUser(boolean isUserText){
        isUser.add(isUserText);
        for (Boolean element : isUser) {
            Log.e("isUserTag:","element="+element);
        }
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {

        if (convertView == null) {
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.list_item_layout, parent, false);
        }


        TextView messageTextView = convertView.findViewById(R.id.messageTextView);

        // Check if the message is from the user
        if (isUser.get(position)) {
            // Set layout for user's message (right-justified)
            LinearLayout chatBubbleLayout = convertView.findViewById(R.id.chatBubbleLayout);
            chatBubbleLayout.setGravity(Gravity.END);// Right-justify the bubble
            messageTextView.setBackgroundResource(R.drawable.user_chat_bubble);
            messageTextView.setTextColor(Color.BLACK); // Example: Change text color for user messages
        } else {
            // Set layout for chatbot's message (left-justified)
            LinearLayout chatBubbleLayout = convertView.findViewById(R.id.chatBubbleLayout);
            chatBubbleLayout.setGravity(Gravity.START); // Left-justify the bubble
            messageTextView.setBackgroundResource(R.drawable.bot_chat_bubble);
            messageTextView.setTextColor(Color.WHITE); // Example: Change text color for chatbot messages
        }

        messageTextView.setText(getItem(position));

        return convertView;
    }

}
