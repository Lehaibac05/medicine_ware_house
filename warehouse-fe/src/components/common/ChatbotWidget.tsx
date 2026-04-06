import { useMemo, useState } from "react";
import { Button, Input, Spin, Typography, message } from "antd";
import { MessageOutlined, SendOutlined, CloseOutlined } from "@ant-design/icons";
import { askChatbot } from "../../services/chat";
import { getAuthToken } from "../../utils/auth";

const { Text } = Typography;

type ChatMessage = {
  role: "user" | "bot";
  text: string;
};

function ChatbotWidget() {
  const [messageApi, contextHolder] = message.useMessage();
  const [open, setOpen] = useState(false);
  const [sending, setSending] = useState(false);
  const [input, setInput] = useState("");
  const [messages, setMessages] = useState<ChatMessage[]>([
    {
      role: "bot",
      text: "Xin chào, mình là trợ lý kho. Bạn có thể hỏi về cảnh báo, tồn kho thấp, hoặc lô sắp hết hạn.",
    },
  ]);

  const isAuthenticated = useMemo(() => !!getAuthToken(), []);

  if (!isAuthenticated) {
    return null;
  }

  const sendMessage = async (preset?: string) => {
    const content = (preset ?? input).trim();
    if (!content || sending) {
      return;
    }

    setMessages((prev) => [...prev, { role: "user", text: content }]);
    setInput("");

    try {
      setSending(true);
      const response = await askChatbot(content);
      const text = response.answer || "Mình chưa có dữ liệu để trả lời.";
      setMessages((prev) => [...prev, { role: "bot", text }]);
    } catch (error) {
      console.error("Chatbot error:", error);
      messageApi.error("Không thể kết nối chatbot");
      setMessages((prev) => [
        ...prev,
        { role: "bot", text: "Tạm thời không kết nối được chatbot. Bạn thử lại sau nhé." },
      ]);
    } finally {
      setSending(false);
    }
  };

  return (
    <>
      {contextHolder}
      {open && (
        <div className="fixed bottom-24 right-6 z-50 w-[360px] max-w-[calc(100vw-24px)] rounded-2xl border border-slate-200 bg-white shadow-[0_16px_36px_rgba(15,23,42,0.16)]">
          <div className="flex items-center justify-between border-b border-slate-200 px-4 py-3">
            <div>
              <Text className="block text-sm font-semibold text-slate-800">Trợ lý kho thuốc</Text>
              <Text className="text-xs text-slate-500">Hỏi nhanh dữ liệu vận hành</Text>
            </div>
            <Button type="text" icon={<CloseOutlined />} onClick={() => setOpen(false)} />
          </div>

          <div className="max-h-[320px] space-y-2 overflow-y-auto px-4 py-3">
            {messages.map((item, index) => (
              <div
                key={`${item.role}-${index}`}
                className={item.role === "user" ? "flex justify-end" : "flex justify-start"}
              >
                <div
                  className={
                    item.role === "user"
                      ? "max-w-[82%] rounded-2xl bg-sky-600 px-3 py-2 text-sm text-white"
                      : "max-w-[82%] rounded-2xl bg-slate-100 px-3 py-2 text-sm text-slate-700"
                  }
                >
                  {item.text}
                </div>
              </div>
            ))}
            {sending && (
              <div className="flex items-center gap-2 text-xs text-slate-500">
                <Spin size="small" />
                <span>Đang xử lý...</span>
              </div>
            )}
          </div>

          <div className="space-y-2 border-t border-slate-200 px-4 py-3">
            <div className="flex flex-wrap gap-2">
              <Button size="small" onClick={() => sendMessage("Tổng quan cảnh báo hôm nay")}>Tổng quan cảnh báo</Button>
              <Button size="small" onClick={() => sendMessage("Có bao nhiêu thuốc tồn kho thấp?")}>Tồn kho thấp</Button>
              <Button size="small" onClick={() => sendMessage("Danh sách lô sắp hết hạn")}>Sắp hết hạn</Button>
              <Button size="small" onClick={() => sendMessage("Scan cảnh báo")}>Scan cảnh báo</Button>
              <Button size="small" onClick={() => sendMessage("Đề xuất nhập hàng")}>Đề xuất nhập</Button>
              <Button size="small" onClick={() => sendMessage("Xác nhận đề xuất nhập hàng")}>Xác nhận đề xuất</Button>
            </div>

            <Input
              value={input}
              onChange={(e) => setInput(e.target.value)}
              onPressEnter={() => sendMessage()}
              placeholder="Nhập câu hỏi..."
              suffix={
                <Button
                  type="text"
                  icon={<SendOutlined />}
                  onClick={() => sendMessage()}
                  disabled={sending || !input.trim()}
                />
              }
            />
          </div>
        </div>
      )}

      <Button
        type="primary"
        shape="circle"
        size="large"
        icon={<MessageOutlined />}
        onClick={() => setOpen((prev) => !prev)}
        className="!fixed bottom-6 right-6 z-50 !h-14 !w-14"
      />
    </>
  );
}

export default ChatbotWidget;
