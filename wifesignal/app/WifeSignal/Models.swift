import Foundation
import SwiftUI

enum SignalColor: String, Codable, CaseIterable, Identifiable {
    case green, yellow, red
    var id: String { rawValue }
    var title: String {
        switch self {
        case .green: return "Whenever"
        case .yellow: return "Soon"
        case .red: return "Now"
        }
    }
    var label: String {
        switch self {
        case .green: return "When you're free"
        case .yellow: return "Soon please"
        case .red: return "It's urgent"
        }
    }
    var isAvailable: Bool { true }
    var color: Color {
        switch self {
        case .green: return Color(red: 0.36, green: 0.67, blue: 0.53)   // sage
        case .yellow: return Color(red: 0.95, green: 0.72, blue: 0.31)  // honey
        case .red: return Color(red: 0.88, green: 0.36, blue: 0.47)     // rose
        }
    }
    var symbol: String {
        switch self {
        case .green: return "leaf.fill"
        case .yellow: return "clock.fill"
        case .red: return "bolt.heart.fill"
        }
    }
}

struct SignalStatus: Codable {
    let color: SignalColor?
    let acknowledged: Bool
    let sentAt: String?
    let acknowledgedAt: String?
    let message: String?
}

struct SignalRequest: Codable { let color: SignalColor }
