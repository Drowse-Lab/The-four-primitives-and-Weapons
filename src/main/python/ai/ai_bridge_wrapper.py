"""
AI Bridge Wrapper - PythonとJavaを連携するためのJSONラッパー

このスクリプトは標準入力からJSONを受け取り、
対応するAIを実行して、JSONで結果を返します。
"""

import sys
import json
import traceback
from typing import Dict, Any
from CommonSoldier import create_ai as create_common_soldier_ai
from EliteSoldier import create_ai as create_elite_soldier_ai
from singularity import create_ai as create_singularity_ai
from HeroicTier import create_ai as create_heroic_ai
from MythicalTier import create_ai as create_mythical_ai
from AngelTier import create_ai as create_angel_ai
from DivineTier import create_ai as create_divine_ai

# AIクラスのマッピング（ティアごとに対応するAI作成関数）
AI_FACTORY_MAP = {
    1: create_common_soldier_ai,  # 一般兵（ティア1）
    2: create_elite_soldier_ai,   # エリート兵（ティア2）
    3: create_singularity_ai,     # 特異点（ティア3）
    4: create_heroic_ai,          # 英雄級（ティア4）
    5: create_mythical_ai,        # 神話級（ティア5）
    6: create_angel_ai,           # 天使級（ティア6）
    7: create_divine_ai,          # 神聖級（ティア7）
}


class AIBridgeWrapper:
    """
    PythonのAIとJavaを橋渡しするラッパークラス

    JSON通信プロトコル:
    入力:
    {
        "command": "initialize" | "update" | "destroy",
        "ai_id": "unique_ai_id",
        "tier": 1,
        "entity_data": {...},
        "world_data": {...},
        "delta_time": 0.05
    }

    出力:
    {
        "status": "success" | "error",
        "ai_id": "unique_ai_id",
        "action": {...},
        "error_message": "..."
    }
    """

    def __init__(self):
        self.active_ais: Dict[str, Any] = {}

    def handle_command(self, request: Dict) -> Dict:
        """コマンドを処理"""
        command = request.get("command", "")
        ai_id = request.get("ai_id", "")

        try:
            if command == "initialize":
                return self._handle_initialize(ai_id, request)
            elif command == "update":
                return self._handle_update(ai_id, request)
            elif command == "destroy":
                return self._handle_destroy(ai_id)
            else:
                return self._error_response(ai_id, f"Unknown command: {command}")

        except Exception as e:
            error_msg = f"Error handling command '{command}': {str(e)}\n{traceback.format_exc()}"
            return self._error_response(ai_id, error_msg)

    def _handle_initialize(self, ai_id: str, request: Dict) -> Dict:
        """AIを初期化"""
        tier = request.get("tier", 1)
        entity_data = request.get("entity_data", {})

        # ティアに対応するAIファクトリーを取得
        ai_factory = AI_FACTORY_MAP.get(tier)
        if ai_factory is None:
            return self._error_response(ai_id, f"No AI implementation for tier {tier}")

        # AIインスタンスを作成
        ai_instance = ai_factory(entity_data)
        self.active_ais[ai_id] = ai_instance

        return {
            "status": "success",
            "ai_id": ai_id,
            "message": f"Initialized AI tier {tier} for {ai_id}"
        }

    def _handle_update(self, ai_id: str, request: Dict) -> Dict:
        """AIを更新"""
        if ai_id not in self.active_ais:
            return self._error_response(ai_id, f"AI not found: {ai_id}")

        ai_instance = self.active_ais[ai_id]
        delta_time = request.get("delta_time", 0.05)
        world_data = request.get("world_data", {})
        entity_data = request.get("entity_data", {})

        # エンティティデータを更新
        ai_instance.entity_data.update(entity_data)

        # AIを更新
        action = ai_instance.update(delta_time, world_data)

        return {
            "status": "success",
            "ai_id": ai_id,
            "action": action,
            "state": ai_instance.current_state.value if hasattr(ai_instance, 'current_state') else "unknown"
        }

    def _handle_destroy(self, ai_id: str) -> Dict:
        """AIを破棄"""
        if ai_id in self.active_ais:
            del self.active_ais[ai_id]
            return {
                "status": "success",
                "ai_id": ai_id,
                "message": f"Destroyed AI {ai_id}"
            }
        else:
            return self._error_response(ai_id, f"AI not found: {ai_id}")

    def _error_response(self, ai_id: str, error_message: str) -> Dict:
        """エラーレスポンスを生成"""
        return {
            "status": "error",
            "ai_id": ai_id,
            "error_message": error_message
        }


def main():
    """メインループ - 標準入力からJSONを読み、処理して標準出力に返す"""
    wrapper = AIBridgeWrapper()

    # 起動完了メッセージ
    print(json.dumps({"status": "ready", "message": "AI Bridge Wrapper initialized"}), flush=True)

    # メインループ
    for line in sys.stdin:
        try:
            # 空行はスキップ
            if not line.strip():
                continue

            # JSONをパース
            request = json.loads(line)

            # コマンドを処理
            response = wrapper.handle_command(request)

            # レスポンスをJSON形式で出力
            print(json.dumps(response), flush=True)

        except json.JSONDecodeError as e:
            error_response = {
                "status": "error",
                "error_message": f"JSON decode error: {str(e)}"
            }
            print(json.dumps(error_response), flush=True)

        except Exception as e:
            error_response = {
                "status": "error",
                "error_message": f"Unexpected error: {str(e)}\n{traceback.format_exc()}"
            }
            print(json.dumps(error_response), flush=True)


if __name__ == "__main__":
    main()
