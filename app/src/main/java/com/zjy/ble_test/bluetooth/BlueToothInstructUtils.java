package com.zjy.ble_test.bluetooth;


public class BlueToothInstructUtils {

	/**指令开始符*/
	private static final String START_STRING = "1F";
	/**指令结束符*/
	private static final String END_STRING = "0000";
	/**缓存指令*/
	public static StringBuilder stringBuilderCache=new StringBuilder();

	/**检测指令是否完整
	 * @param instruct：需要检测的指令
	 * @return true：是完整指令
	 */
	public static boolean checkString(String instruct) {
		instruct=instruct.replace(" ","");//去空格
		if(instruct.length()>=18
				&&instruct.startsWith(START_STRING)
				&&instruct.endsWith(END_STRING)){
			stringBuilderCache.delete(0, stringBuilderCache.length());//清空缓存
			return true;
		}
		return false;
	}

	/**格式化指令
	 * @param currentInstruct 当前获取的指令
	 * @return 完整指令orNull
	 */
	public static String[] formatString(String currentInstruct) {
		String[] instructs=new String[3];//存放返回的指令
		currentInstruct = currentInstruct.replace(" ","");//去空格
		stringBuilderCache.append(currentInstruct);//拼接上一次剩余指令
		String appendInstruct = stringBuilderCache.toString();
		stringBuilderCache.delete(0, stringBuilderCache.length());//清空缓存
		int countStr = countStr(appendInstruct,END_STRING);//获取结束符个数，既指令个数
		//无完整指令
		if(countStr==0){
			if(appendInstruct.startsWith(START_STRING)){//判断是否以启动符开头
				stringBuilderCache.append(appendInstruct);
			}
			return null;
		}
		//有一条完整指令
		if(countStr==1){
			appendInstruct = saveCache(appendInstruct);
			instructs[0]=appendInstruct;
			return instructs;
		}
		//有两条或三条完整指令
		if(countStr>1){
			appendInstruct = saveCache(appendInstruct);
			//截取指令
			int start,end;
			for(int i=0;i<countStr;i++){
				start = appendInstruct.indexOf(START_STRING);//获取第一条指令的结束符角标
				end = appendInstruct.indexOf(END_STRING)+END_STRING.length();//获取第一条指令的结束符角标
				instructs[i]=appendInstruct.substring(start, end);//获取一条完整的指令
				appendInstruct=appendInstruct.substring(end);//截取未处理的指令
			}
			return instructs;
		}
		return null;
	}

	/**保存剩余指令，并输出完整指令
	 * @param appendInstruct 可能包含剩余指令的字符串
	 * @return 完整指令
	 */
	private static String saveCache(String appendInstruct) {
		if(!appendInstruct.endsWith(END_STRING)){//结尾有剩余指令需要缓存
			int indexOfEnd = appendInstruct.lastIndexOf(END_STRING)+ END_STRING.length();//获取结束符角标
			stringBuilderCache.append(appendInstruct.substring(indexOfEnd));//缓存剩余指令
			appendInstruct=appendInstruct.substring(0, indexOfEnd);//截取完整指令
		}
		return appendInstruct;
	}

	/**获取父string中包含子string的个数
	 * @param longString 父string
	 * @param str 子string
	 * @return 数量
	 */
	public static int countStr(String longString, String str) {
		int counter=0;
		int indexOfLong = longString.indexOf(str);
		if (indexOfLong != -1) {
			counter++;
			counter+=countStr(longString.substring(indexOfLong +
					str.length()), str);  //截取未判断部分
		}
		return counter;
	}
}
