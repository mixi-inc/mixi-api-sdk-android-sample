
package org.example.mixi;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import jp.mixi.android.sdk.CallbackListener;
import jp.mixi.android.sdk.Config;
import jp.mixi.android.sdk.ErrorInfo;
import jp.mixi.android.sdk.HttpMethod;
import jp.mixi.android.sdk.MixiContainer;
import jp.mixi.android.sdk.MixiContainerFactory;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

/**
 * mixi Graph API SDK for Android を用いて、ログイン、つぶやきの一覧と投稿を行うサンプルです。
 * 
 * @author yuki.fujisaki
 */
public class MixiAndroidExampleActivity extends Activity implements OnItemClickListener {

    /** Consumer Key (登録したアプリケーションの Consumer Key を設定) */
    private static final String CLIENT_ID = "";

    /** このアプリケーションで認可を求めるパーミッション */
    private static final String[] PERMISSIONS = new String[] {
            "r_profile",	// プロフィール情報の取得
            "r_voice",		// つぶやきの取得
            "w_voice"		// つぶやきの投稿
    };

    /** mixi API SDK で使用する onActivityResult のコールバック識別子(任意の値) */
    private static final int REQUEST_MIXI_API = 3941;

    /** mixi API SDK のインタフェース */
    private MixiContainer mContainer;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_INDETERMINATE_PROGRESS);
        setContentView(R.layout.main);
        
        // 要素がタップされた際のイベントハンドラをセット
		ListView listView = (ListView) findViewById(R.id.list);
		listView.setOnItemClickListener(this);
        
        initMixiContainer();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        closeMixiContainer();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // callback を MixiContainer にも伝搬
        mContainer.authorizeCallback(requestCode, resultCode, data);
    }

    /**
     * main.xml の "ログイン" ボタン押下に対応するコールバック。
     * 
     * @param v ログインボタンの View インスタンス
     */
    public void onLoginClick(View v) {
    	
    	// 既に認可済み
    	if (mContainer.isAuthorized()) {
    		onInitialized();
    		return;
    	}
    	
    	// 認可処理を開始
        mContainer.authorize(this, PERMISSIONS, REQUEST_MIXI_API, new CallbackListener() {
            @Override
            public void onComplete(Bundle values) {
                onInitialized();
            }

            @Override
            public void onFatal(ErrorInfo e) {
                showToast("エラーが発生しました");
            }

            @Override
            public void onError(ErrorInfo e) {
                showToast("エラーが発生しました");
            }

            @Override
            public void onCancel() {
                showToast("中止しました");
            }
        });
    }

    /**
     * 投稿ボタンが押された際に呼び出されるコールバック。
     * 
     * @param v
     */
    public void onPostClick(View v) {
        TextView textView = (TextView) findViewById(R.id.postEditText);
        setPostFormEnabled(false);
        
        final String message = textView.getText().toString();
        if (TextUtils.isEmpty(message)) {
            // テキストが空
            showToast("メッセージが入力されていません");
            setPostFormEnabled(true);
            return;
        }

        textView.setText(null);
        postVoice(message);
    }

    /**
     * つぶやきを送信する。
     * 
     * @param message 投稿するつぶやきの内容
     * @see <a
     *      href="http://developer.mixi.co.jp/connect/mixi_graph_api/mixi_io_spec_top/voice-api/#toc-8">つぶやきの投稿と削除</a>
     */
    private void postVoice(final String message) {
        HashMap<String, String> parameters = new HashMap<String, String>();
        parameters.put("status", message);
        setProgressBarIndeterminateVisibility(true);
        mContainer.send("/voice/statuses/update", HttpMethod.POST, parameters,
                new CallbackListener() {
                    @Override
                    public void onComplete(Bundle values) {
                        showToast("投稿に成功しました");
                        setPostFormEnabled(true);
                        setProgressBarIndeterminateVisibility(false);
                        
                        // 再読込
                        clearVoiceList();
                        loadVoice(0);
                    }

                    @Override
                    public void onFatal(ErrorInfo e) {
                        showToast("エラーが発生しました");
                        setPostFormEnabled(true);
                        setProgressBarIndeterminateVisibility(false);
                    }

                    @Override
                    public void onError(ErrorInfo e) {
                        showToast("エラーが発生しました");
                        setPostFormEnabled(true);
                        setProgressBarIndeterminateVisibility(false);
                    }

                    @Override
                    public void onCancel() {
                        showToast("中止しました");
                        setPostFormEnabled(true);
                        setProgressBarIndeterminateVisibility(false);
                    }
                });
    }

	/** 投稿フォームの有効/無効を設定する */
	protected void setPostFormEnabled(boolean b) {
		findViewById(R.id.postEditText).setEnabled(b);
		findViewById(R.id.postButton).setEnabled(b);
	}

	/**
     * mixi API SDK を初期化する。終了 (onDestroy) 前に {@link #closeMixiContainer()}
     * で解放が必要。
     */
    private void initMixiContainer() {
        Config config = new Config();
        config.clientId = CLIENT_ID;
        config.selector = Config.GRAPH_API;
        mContainer = MixiContainerFactory.getContainer(config);
        mContainer.init(this);
    }

    /**
     * mixi API SDK の終了処理を行い、各種リソースを解放する。
     */
    private void closeMixiContainer() {
        mContainer.close(this);
    }

    /**
     * 初期化が完了し、ログイン済みの状態となり、 API コールが可能になった際に呼び出されるコールバック。
     */
    protected void onInitialized() {
    	// ログインボタンを無効化
        findViewById(R.id.loginButton).setEnabled(false);

        // 投稿ボタンを有効にする
        setPostFormEnabled(true);
        
        // 自分のプロフィールを取得する
        loadSelfProfile();

        // ボイスを読み込む
        loadVoice(0);
    }
    
    MixiVoiceListAdapter mListAdapter = null;
    
	/** ListView から Adapter を取り外し、リストをクリアする。 **/
	protected void clearVoiceList() {
		ListView listView = (ListView) findViewById(R.id.list);
		listView.setAdapter(null);
		listView.removeFooterView(getListFooterView());
		mListAdapter = null;
	}

	/**
	 * MixiVoice のリストを ListView に追加する。 Adapter がまだない場合は、自動的に
	 * MixiVoiceListAdapter を作成する。
	 * 
	 * @param voices
	 *            MixiVoice のリスト。
	 */
    protected void addVoicesToList(ArrayList<MixiVoice> voices) {
		ListView listView = (ListView) findViewById(R.id.list);
        if (mListAdapter == null) {
        	// "もっと見る..." をリスト末尾に追加
        	listView.addFooterView(getListFooterView());
	
	        // ListAdapter を新規に作って、 ListView にセットする
        	mListAdapter = new MixiVoiceListAdapter(this, voices);
	        listView.setAdapter(mListAdapter);
        } else {
        	// 既存のアダプタへ追加する
        	for (MixiVoice voice : voices) {
        		mListAdapter.add(voice);
        	}
        	// "もっと見る..." を有効に戻す
			getListFooterView().setEnabled(true);
        }
    }

    private View mListFooterView = null;

	private View getListFooterView() {
		if (mListFooterView == null) {
			mListFooterView = getLayoutInflater().inflate(
					android.R.layout.simple_list_item_1, null);
			TextView textView = (TextView) mListFooterView
					.findViewById(android.R.id.text1);
			textView.setText("もっと見る...");
		}
		return mListFooterView;
	}

	/**
     * つぶやき一覧の取得処理を開始する。この処理は非同期で実行されるため、取得完了を待たずに即座に呼び出し元に処理が戻る。
     * 
     * @see <a
     *      href="http://developer.mixi.co.jp/connect/mixi_graph_api/mixi_io_spec_top/voice-api/#toc-3">友人のつぶやき一覧の取得</a>
     */
	protected void loadVoice(int startIndex) {
		HashMap<String, String> options = new HashMap<String, String>();
		options.put("startIndex", String.valueOf(startIndex));
        setProgressBarIndeterminateVisibility(true);

		mContainer.send("/voice/statuses/friends_timeline", options, new CallbackListener() {
			@Override
			public void onComplete(Bundle values) {
				// values の response の中に、つぶやきの取得結果が返ってきます。
				String result = values.getString("response");

				// 結果の JSON を解析してつぶやきを取り出す
				ArrayList<MixiVoice> voices = new ArrayList<MixiVoice>();
				try {
					JSONArray array = new JSONArray(result);
					int length = array.length();
					for (int i = 0; i < length; i++) {
						JSONObject item = array.getJSONObject(i);
						JSONObject user = item.getJSONObject("user");

						// つぶやき1エントリを追加
						MixiVoice voice = new MixiVoice();
						voice.text = item.getString("text");
						voice.screenName = user.getString("screen_name");

						voices.add(voice);
					}
				} catch (JSONException e) {
					showToast("レスポンスのJSONを解析できませんでした");
				}

				// リストにセット
				addVoicesToList(voices);

				setProgressBarIndeterminateVisibility(false);
			}

			@Override
			public void onFatal(ErrorInfo e) {
				showToast("つぶやきの取得中にエラーが発生しました");
                setProgressBarIndeterminateVisibility(false);
			}

			@Override
			public void onError(ErrorInfo e) {
				showToast("つぶやきの取得中にエラーが発生しました");
                setProgressBarIndeterminateVisibility(false);
			}

			@Override
			public void onCancel() {
				showToast("つぶやきの取得を中止しました");
                setProgressBarIndeterminateVisibility(false);
			}
		});
	}

	/**
	 * People API を呼び出し、自分のプロフィール情報を取得する。
	 * 
     * @see <a href="http://developer.mixi.co.jp/connect/mixi_graph_api/mixi_io_spec_top/people-api/">People API</a>
	 */
    private void loadSelfProfile() {
        mContainer.send("/people/@me/@self", new CallbackListener() {
            @Override
            public void onComplete(Bundle values) {
                // response の中に、取得結果の JSON が入っています
                String result = values.getString("response");
            	String nickname;
                
            	// 結果の JSON を解析してプロフィール情報を取り出す
                try {
                    JSONObject json = new JSONObject(result);
                    
                    JSONObject entry = json.getJSONObject("entry");
                    nickname = entry.optString("displayName");
                } catch (JSONException e) {
                    showToast("レスポンスのJSONを解析できませんでした");
                    return;
                }
                
                setTitle(nickname);
            }

            @Override
            public void onFatal(ErrorInfo e) {
                showToast("プロフィールの取得中にエラーが発生しました");
            }

            @Override
            public void onError(ErrorInfo e) {
                showToast("プロフィールの取得中にエラーが発生しました");
            }

            @Override
            public void onCancel() {
                showToast("プロフィールの取得を中止しました");
            }
        });
	}


    /**
     * 画面上に指定されたメッセージの Toast を表示する　
     * 
     * @param text 表示するメッセージ
     */
    private void showToast(String text) {
        Toast.makeText(this, text, Toast.LENGTH_SHORT).show();
    }

    /**
     * MixiVoice をリスト上の要素に割り当てる Adapter 実装。
     * 
     * @author yuki.fujisaki
     */
    public static class MixiVoiceListAdapter extends ArrayAdapter<MixiVoice> {
        public MixiVoiceListAdapter(Context context, List<MixiVoice> list) {
            super(context, android.R.layout.simple_list_item_2, android.R.id.text1, list);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // リストの1行を展開する際に呼び出されるので、
            // レイアウト上 (ここでは simple_list_item_2.xml 上) の要素にセットする
            View view = super.getView(position, convertView, parent);

            TextView text1 = (TextView) view.findViewById(android.R.id.text1);
            TextView text2 = (TextView) view.findViewById(android.R.id.text2);

            MixiVoice item = getItem(position);
            text1.setText(item.screenName);
            text2.setText(item.text);

            return view;
        }
    }

    /**
     * つぶやき 1 エントリを表現する要素。
     * 
     * @author yuki.fujisaki
     */
    public static class MixiVoice {
        public String text;
        public String screenName;
    }

	/**
	 * リスト上のエントリがタップされたときのイベントハンドラ
	 */
	@Override
	public void onItemClick(AdapterView<?> listView, View itemView,
			int position, long itemId) {
		int count = listView.getCount();
		if (position == count - 1) {
			// footer がタップされた: 続きを取得してみる
			View listFooterView = getListFooterView();
			if (listFooterView.isEnabled()) {
				loadVoice(mListAdapter.getCount());
				listFooterView.setEnabled(false);
			}
		} else {
			// つぶやきが押された: Toast で本文を表示してみる
			MixiVoice voice = (MixiVoice) listView.getItemAtPosition(position);
			showToast(voice.text);
		}
	}
}
