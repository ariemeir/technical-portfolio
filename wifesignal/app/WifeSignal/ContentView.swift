import SwiftUI

struct ContentView: View {
    @EnvironmentObject private var model: SignalViewModel
    @Environment(\.colorScheme) private var colorScheme

    private var backgroundGradient: LinearGradient {
        let colors = colorScheme == .dark
            ? [Color(red: 0.16, green: 0.12, blue: 0.18), Color(red: 0.10, green: 0.10, blue: 0.15)]
            : [Color(red: 1.00, green: 0.94, blue: 0.96), Color(red: 0.94, green: 0.93, blue: 1.00)]
        return LinearGradient(colors: colors, startPoint: .topLeading, endPoint: .bottomTrailing)
    }

    var body: some View {
        NavigationStack {
            ZStack {
                backgroundGradient.ignoresSafeArea()
                VStack(spacing: 20) {
                    Text("Send Arie a signal")
                        .font(.title2.bold())
                        .padding(.top, 4)

                    ForEach(SignalColor.allCases) { signal in
                        signalButton(signal)
                    }

                    statusCard

                    Button {
                        Task { await model.clear() }
                    } label: {
                        Text("Clear signal")
                            .font(.subheadline.weight(.semibold))
                            .padding(.horizontal, 6)
                    }
                    .buttonStyle(.bordered)
                    .buttonBorderShape(.capsule)
                    .tint(SignalColor.red.color)
                    .disabled(model.isLoading || model.status?.color == nil)

                    Spacer(minLength: 0)
                }
                .padding(20)
            }
            .fontDesign(.rounded)
            .toolbar {
                ToolbarItem(placement: .topBarTrailing) {
                    Button { model.showSettings = true } label: { Image(systemName: "gearshape") }
                }
            }
            .sheet(isPresented: $model.showSettings) { SettingsView(settings: model.settings) }
            .overlay {
                if model.isLoading {
                    ProgressView()
                        .controlSize(.large)
                        .padding()
                        .background(.regularMaterial, in: RoundedRectangle(cornerRadius: 16))
                }
            }
        }
    }

    private func signalButton(_ signal: SignalColor) -> some View {
        let isActive = model.status?.color == signal
        return Button {
            Task { await model.send(signal) }
        } label: {
            HStack(spacing: 16) {
                Image(systemName: signal.symbol)
                    .font(.system(size: 24, weight: .semibold))
                    .frame(width: 52, height: 52)
                    .background(.white.opacity(0.28), in: Circle())
                VStack(alignment: .leading, spacing: 2) {
                    Text(signal.title)
                        .font(.title3.bold())
                        .lineLimit(1)
                    Text(signal.label)
                        .font(.subheadline)
                        .opacity(0.92)
                        .lineLimit(1)
                        .minimumScaleFactor(0.8)
                }
                Spacer(minLength: 0)
                if isActive {
                    Image(systemName: "checkmark.circle.fill")
                        .font(.system(size: 22, weight: .semibold))
                        .opacity(0.9)
                }
            }
            .foregroundStyle(.white)
            .padding(.horizontal, 18)
            .frame(maxWidth: .infinity, minHeight: 82)
            .background(
                LinearGradient(
                    colors: [signal.color.opacity(0.95), signal.color.opacity(0.75)],
                    startPoint: .topLeading, endPoint: .bottomTrailing
                ),
                in: RoundedRectangle(cornerRadius: 26, style: .continuous)
            )
            .overlay(
                RoundedRectangle(cornerRadius: 26, style: .continuous)
                    .strokeBorder(.white.opacity(isActive ? 0.7 : 0.25), lineWidth: isActive ? 2 : 1)
            )
            .shadow(color: signal.color.opacity(0.35), radius: 8, y: 4)
        }
        .buttonStyle(.plain)
        .disabled(model.isLoading || !signal.isAvailable)
    }

    private var statusCard: some View {
        VStack(spacing: 8) {
            if let error = model.errorMessage {
                Label(error, systemImage: "wifi.exclamationmark")
                    .foregroundStyle(SignalColor.red.color)
                    .font(.footnote)
                    .multilineTextAlignment(.center)
            } else if let status = model.status, let color = status.color {
                HStack(spacing: 10) {
                    Circle().fill(color.color).frame(width: 12, height: 12)
                    Text("\(color.title) — sent")
                        .font(.headline)
                        .lineLimit(1)
                    Spacer()
                    if status.acknowledged {
                        Label("Seen", systemImage: "heart.fill")
                            .font(.subheadline.weight(.semibold))
                            .foregroundStyle(SignalColor.red.color)
                    } else {
                        Text("Waiting…")
                            .font(.subheadline)
                            .foregroundStyle(.secondary)
                    }
                }
            } else {
                Text(model.settings.isConfigured ? "No active signal" : "Tap the gear to connect the app")
                    .font(.subheadline)
                    .foregroundStyle(.secondary)
            }
        }
        .padding(16)
        .frame(maxWidth: .infinity)
        .background(.thinMaterial, in: RoundedRectangle(cornerRadius: 20, style: .continuous))
    }
}
